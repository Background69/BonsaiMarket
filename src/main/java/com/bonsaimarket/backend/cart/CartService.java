package com.bonsaimarket.backend.cart;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.product.Product;
import com.bonsaimarket.backend.product.ProductStatus;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.user.UserRepository;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import static com.bonsaimarket.backend.cart.CartDtos.*;

/**
 * Cart management. The owner is always the authenticated user passed in by the controller, never a request field.
 * Every mutation locks the user row first, so concurrent requests of one buyer are serialized and the
 * UNIQUE (cart_id, product_id) rule can never be hit by a race. Stock is only checked here, not reserved:
 * {@code CheckoutService} re-validates and locks products when the order is reviewed and placed.
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CartService {
    private final ObjectProvider<EntityManager> entityManagers;
    private final UserRepository users;
    private final Clock clock;

    public CartService(ObjectProvider<EntityManager> entityManagers, UserRepository users, Clock clock) {
        this.entityManagers = entityManagers;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public View view(Long userId) {
        buyer(userId, false);
        return render(findCart(userId));
    }

    /** Checkout shares the same authenticated buyer rule and user-row lock as cart mutations. */
    @Transactional(readOnly = true)
    public User buyer(Long userId) { return buyer(userId, false); }

    public Cart locked(Long userId) {
        buyer(userId, true);
        Cart cart = findCart(userId);
        if (cart == null) { cart = createCart(userId); em().flush(); }
        return cart;
    }

    /** Called only with the server-owned cart returned by locked; product order matches checkout locks. */
    public List<CartItem> items(Cart cart) {
        return em().createQuery("select i from CartItem i join fetch i.product p join fetch p.store where i.cart.id=:id order by p.id", CartItem.class)
                .setParameter("id", cart.getId()).setMaxResults(MAX_LINES + 1).getResultList();
    }

    public View add(Long userId, Long productId, int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) throw MarketplaceException.invalid("Số lượng không hợp lệ.");
        return add(userId, new AddRequest(productId, quantity));
    }

    public View update(Long userId, Long cartItemId, int quantity) {
        return quantity == 0 ? remove(userId, cartItemId) : updateQuantity(userId, cartItemId, quantity);
    }

    /** Checkout calls this after acquiring its product lock; reuse Cart's current availability rules. */
    public void validate(Product product, int quantity) {
        requireSellable(product);
        if (quantity < 1 || quantity > MAX_QUANTITY) throw MarketplaceException.invalid("Số lượng không hợp lệ.");
        requireStock(product, quantity, 0);
    }

    public View add(Long userId, AddRequest request) {
        buyer(userId, true);
        Product product = em().find(Product.class, request.productId());
        if (product == null) throw MarketplaceException.missing();
        requireSellable(product);
        Cart cart = findCart(userId);
        if (cart == null) cart = createCart(userId);
        CartItem line = findLine(cart.getId(), product.getId());
        int inCart = line == null ? 0 : line.getQuantity();
        long wanted = (long) inCart + request.quantity();
        if (wanted > MAX_QUANTITY) throw MarketplaceException.invalid("Số lượng trong giỏ vượt quá giới hạn cho phép.");
        requireStock(product, wanted, inCart);
        if (line == null) {
            if (countLines(cart.getId()) >= MAX_LINES)
                throw conflict("CART_FULL", "Giỏ hàng tối đa " + MAX_LINES + " sản phẩm khác nhau.");
            line = new CartItem();
            line.setCart(cart);
            line.setProduct(product);
            line.setQuantity((int) wanted);
            em().persist(line);
        } else {
            line.setQuantity((int) wanted);
        }
        cart.setUpdatedAt(now());
        em().flush();
        return render(cart);
    }

    public View updateQuantity(Long userId, Long cartItemId, int quantity) {
        buyer(userId, true);
        if (quantity < 1 || quantity > MAX_QUANTITY) throw MarketplaceException.invalid("Số lượng không hợp lệ.");
        CartItem line = ownedLine(userId, cartItemId);
        Product product = line.getProduct();
        requireSellable(product);
        requireStock(product, quantity, 0);
        line.setQuantity(quantity);
        line.getCart().setUpdatedAt(now());
        em().flush();
        return render(line.getCart());
    }

    public View remove(Long userId, Long cartItemId) {
        buyer(userId, true);
        CartItem line = ownedLine(userId, cartItemId);
        Cart cart = line.getCart();
        em().remove(line);
        cart.setUpdatedAt(now());
        em().flush();
        return render(cart);
    }

    // ---- lookups: every one is scoped by the owner, so another buyer's ids look like "not found" ----

    private Cart findCart(Long userId) {
        var found = em().createQuery("select c from Cart c where c.user.id=:user", Cart.class)
                .setParameter("user", userId).getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }

    private Cart createCart(Long userId) {
        Cart cart = new Cart();
        cart.setUser(em().getReference(User.class, userId));
        cart.setCreatedAt(now());
        cart.setUpdatedAt(now());
        em().persist(cart);
        return cart;
    }

    private CartItem findLine(Long cartId, Long productId) {
        var found = em().createQuery("select i from CartItem i where i.cart.id=:cart and i.product.id=:product", CartItem.class)
                .setParameter("cart", cartId).setParameter("product", productId).getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }

    private CartItem ownedLine(Long userId, Long cartItemId) {
        var found = em().createQuery("select i from CartItem i where i.id=:id and i.cart.user.id=:user", CartItem.class)
                .setParameter("id", cartItemId).setParameter("user", userId).getResultList();
        if (found.isEmpty()) throw MarketplaceException.missing();
        return found.getFirst();
    }

    private long countLines(Long cartId) {
        return em().createQuery("select count(i) from CartItem i where i.cart.id=:cart", Long.class)
                .setParameter("cart", cartId).getSingleResult();
    }

    // ---- rules ----

    /** Mirrors the sellability rules of CheckoutService.checkedItem, without the locks and without throwing. */
    private boolean sellable(Product p) {
        var store = p.getStore();
        var category = p.getCategory();
        if (p.getStatus() != ProductStatus.ACTIVE || p.getPrice() == null || p.getPrice().signum() < 0) return false;
        if (store == null || store.getStatus() != StoreStatus.ACTIVE || store.getOwner() == null) return false;
        if (!Boolean.TRUE.equals(store.getOwner().getIsActive())
                || !Set.of(UserRole.SELLER, UserRole.ADMIN).contains(store.getOwner().getRole())) return false;
        return category != null && Boolean.TRUE.equals(category.getIsActive());
    }

    private void requireSellable(Product p) {
        if (!sellable(p))
            throw conflict("PRODUCT_UNAVAILABLE", "Sản phẩm hoặc gian hàng không còn được bán.");
    }

    private void requireStock(Product p, long wanted, int alreadyInCart) {
        int stock = p.getStock() == null ? 0 : p.getStock();
        if (stock < 1) throw conflict("OUT_OF_STOCK", "Sản phẩm “" + p.getName() + "” đã hết hàng.");
        if (wanted > stock) {
            throw conflict("OUT_OF_STOCK", alreadyInCart > 0
                    ? "Giỏ hàng đã có " + alreadyInCart + " sản phẩm “" + p.getName() + "”, trong kho chỉ còn " + stock + "."
                    : "Sản phẩm “" + p.getName() + "” chỉ còn " + stock + " trong kho.");
        }
    }

    private User buyer(Long id, boolean lock) {
        User user = (lock ? users.lockById(id) : users.findById(id)).orElseThrow(MarketplaceException::missing);
        if (lock) em().refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getEmailVerifiedAt() == null)
            throw new MarketplaceException(403, "ACCOUNT_UNAVAILABLE", "Vui lòng đăng nhập bằng tài khoản đã xác thực.");
        return user;
    }

    // ---- rendering ----

    private View render(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        if (cart == null) return new View(List.of(), subtotal);
        var rows = em().createQuery("select i from CartItem i join fetch i.product p left join fetch p.store s "
                        + "left join fetch s.owner left join fetch p.category where i.cart.id=:cart order by i.id", CartItem.class)
                .setParameter("cart", cart.getId()).getResultList();
        List<Item> items = new ArrayList<>();
        for (CartItem row : rows) {
            Item item = item(row);
            items.add(item);
            subtotal = subtotal.add(item.lineTotal());
        }
        return new View(items, subtotal);
    }

    private Item item(CartItem row) {
        Product p = row.getProduct();
        BigDecimal price = p.getPrice() == null ? BigDecimal.ZERO.setScale(2) : p.getPrice().setScale(2, RoundingMode.HALF_UP);
        int quantity = row.getQuantity();
        boolean inStock = p.getStock() != null && p.getStock() > 0;
        var store = p.getStore();
        return new Item(row.getId(), p.getId(), p.getName(), p.getImageUrl(),
                store == null ? null : store.getId(), store == null ? null : store.getName(),
                price, quantity, price.multiply(BigDecimal.valueOf(quantity)), p.getStock(), sellable(p) && inStock);
    }

    private MarketplaceException conflict(String code, String message) {
        return new MarketplaceException(409, code, message);
    }

    // Resolve only when the cart is used; public catalog/health smoke contexts intentionally omit JPA.
    private EntityManager em() {
        EntityManager manager = entityManagers.getIfAvailable();
        if (manager == null) throw new MarketplaceException(503, "CART_UNAVAILABLE", "Dịch vụ giỏ hàng chưa sẵn sàng.");
        return manager;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
