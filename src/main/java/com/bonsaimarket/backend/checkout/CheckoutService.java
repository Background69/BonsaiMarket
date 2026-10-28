package com.bonsaimarket.backend.checkout;

import com.bonsaimarket.backend.address.*;
import com.bonsaimarket.backend.cart.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.order.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import static com.bonsaimarket.backend.checkout.CheckoutDtos.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CheckoutService {
    private final ObjectProvider<EntityManager> entityManagers;
    private final UserRepository users;
    private final AddressService addresses;
    private final ObjectMapper json;
    private final Clock clock;
    private final BigDecimal shippingFee;
    private static final BigDecimal MAX_MONEY = new BigDecimal("9999999999999.99");

    public CheckoutService(ObjectProvider<EntityManager> entityManagers, UserRepository users, AddressService addresses, ObjectMapper json,
                           Clock clock, @Value("${checkout.shipping-fee-per-store:30000}") BigDecimal shippingFee) {
        this.entityManagers = entityManagers; this.users = users; this.addresses = addresses; this.json = json; this.clock = clock;
        if (shippingFee.signum() < 0 || shippingFee.compareTo(MAX_MONEY) > 0 || shippingFee.stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("checkout.shipping-fee-per-store must be a nonnegative whole VND amount");
        this.shippingFee = shippingFee.setScale(2);
    }

    @Transactional(readOnly = true)
    public CartView cart(Long buyerId) {
        buyer(buyerId, false);
        List<Item> items = new ArrayList<>();
        for (CartItem row : cartRows(buyerId, false)) items.add(item(row.getProduct(), row.getQuantity(), row.getId()));
        return new CartView(items, addresses.list(buyerId), shippingFee);
    }

    public Quote quote(Long buyerId, Request request) {
        buyer(buyerId, true);
        validate(request);
        long recent = em().createQuery("select count(a) from CheckoutAttempt a where a.buyerUserId=:buyer and a.createdAt>:since", Long.class)
                .setParameter("buyer", buyerId).setParameter("since", now().minusMinutes(10)).getSingleResult();
        if (recent >= 60) throw new MarketplaceException(429, "CHECKOUT_LIMIT", "Bạn đã xem lại đơn nhiều lần. Vui lòng thử lại sau vài phút.");
        Request normalized = new Request(request.source(), request.addressId(), request.productId(), request.quantity(),
                request.notes() == null ? "" : request.notes().strip());
        Review review = review(buyerId, normalized);
        var attempt = new CheckoutAttempt();
        attempt.setId(UUID.randomUUID().toString()); attempt.setBuyerUserId(buyerId);
        attempt.setRequestJson(json.writeValueAsString(normalized)); attempt.setReviewJson(json.writeValueAsString(review));
        attempt.setCreatedAt(now()); attempt.setExpiresAt(now().plusMinutes(15));
        em().persist(attempt);
        return new Quote(attempt.getId(), iso(attempt.getExpiresAt()), review);
    }

    public Receipt submit(Long buyerId, String quoteId) {
        User buyer = buyer(buyerId, true);
        CheckoutAttempt attempt = owned(buyerId, quoteId, true);
        // Check the receipt BEFORE expiry/cart: a lost response must never cause a second order.
        if (attempt.getReceiptJson() != null) return json.readValue(attempt.getReceiptJson(), Receipt.class);
        if (!attempt.getExpiresAt().isAfter(now())) throw conflict("QUOTE_EXPIRED", "Bản xem lại đã hết hạn. Vui lòng xem lại đơn trước khi đặt.");
        Request request = json.readValue(attempt.getRequestJson(), Request.class);
        Review current = review(buyerId, request);
        if (!json.writeValueAsString(current).equals(attempt.getReviewJson()))
            throw conflict("CHECKOUT_CHANGED", "Giỏ hàng, giá hoặc địa chỉ đã thay đổi. Vui lòng xem lại đơn trước khi xác nhận.");

        List<CreatedOrder> result = new ArrayList<>();
        for (StoreTotal group : current.stores()) {
            var order = new Order();
            order.setCode("BSM-" + UUID.randomUUID());
            order.setBuyer(buyer);
            order.setStore(em().getReference(com.bonsaimarket.backend.store.Store.class, group.storeId()));
            order.setShippingAddressSnapshot(json.writeValueAsString(current.address()));
            order.setShippingMethod("STANDARD"); order.setShippingFee(group.shippingFee());
            order.setSubtotal(group.subtotal()); order.setTotal(group.total()); order.setNotes(current.notes());
            // Existing seller workflow owns confirmation and inventory debit. Do not debit twice here.
            order.setStatus(OrderStatus.PENDING); order.setCreatedAt(now()); order.setUpdatedAt(now());
            em().persist(order);
            for (Item item : current.items()) {
                if (!item.storeId().equals(group.storeId())) continue;
                var line = new OrderItem(); line.setOrder(order);
                line.setProduct(em().getReference(Product.class, item.productId())); line.setProductNameSnapshot(item.name());
                line.setUnitPrice(item.unitPrice()); line.setQuantity(item.quantity()); line.setLineTotal(item.lineTotal());
                em().persist(line);
            }
            result.add(new CreatedOrder(order.getId(), order.getCode(), group.storeId(), group.storeName(),
                    order.getStatus().name(), group.subtotal(), group.shippingFee(), group.total()));
        }
        if (request.source() == Source.CART) {
            for (Item item : current.items()) em().remove(em().find(CartItem.class, item.cartItemId()));
        }
        Receipt receipt = new Receipt(quoteId, result, current.total(), iso(now()));
        attempt.setReceiptJson(json.writeValueAsString(receipt));
        em().flush();
        return receipt;
    }

    @Transactional(readOnly = true)
    public Quote savedQuote(Long buyerId, String quoteId) {
        buyer(buyerId, false);
        var attempt = owned(buyerId, quoteId, false);
        return new Quote(quoteId, iso(attempt.getExpiresAt()), json.readValue(attempt.getReviewJson(), Review.class));
    }

    @Transactional(readOnly = true)
    public Receipt receipt(Long buyerId, String quoteId) {
        buyer(buyerId, false);
        var attempt = owned(buyerId, quoteId, false);
        if (attempt.getReceiptJson() == null) throw MarketplaceException.missing();
        return json.readValue(attempt.getReceiptJson(), Receipt.class);
    }

    private Review review(Long buyerId, Request request) {
        var address = addresses.shippingSnapshot(buyerId, request.addressId());
        List<Item> items = new ArrayList<>();
        if (request.source() == Source.CART) {
            var rows = cartRows(buyerId, true);
            if (rows.isEmpty()) throw conflict("CART_EMPTY", "Giỏ hàng chưa có sản phẩm để đặt.");
            // The cart rows and all products are locked in ascending product order.
            for (var row : rows) items.add(checkedItem(row.getProduct().getId(), row.getQuantity(), row.getId()));
        } else items.add(checkedItem(request.productId(), request.quantity(), null));

        Map<Long, List<Item>> groups = new TreeMap<>();
        for (var item : items) groups.computeIfAbsent(item.storeId(), ignored -> new ArrayList<>()).add(item);
        List<StoreTotal> stores = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2), fees = BigDecimal.ZERO.setScale(2);
        for (var group : groups.entrySet()) {
            BigDecimal sum = group.getValue().stream().map(Item::lineTotal).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            BigDecimal total = sum.add(shippingFee);
            if (total.compareTo(MAX_MONEY) > 0) throw MarketplaceException.invalid("Tổng tiền đơn vượt giới hạn.");
            stores.add(new StoreTotal(group.getKey(), group.getValue().getFirst().storeName(), sum, shippingFee, total));
            subtotal = subtotal.add(sum); fees = fees.add(shippingFee);
        }
        return new Review(address, items, stores, subtotal, fees, subtotal.add(fees), request.notes());
    }

    private Item checkedItem(Long productId, int quantity, Long cartItemId) {
        if (quantity < 1 || quantity > 1_000_000) throw MarketplaceException.invalid("Số lượng không hợp lệ.");
        Product product = em().find(Product.class, productId);
        if (product == null) throw conflict("PRODUCT_UNAVAILABLE", "Sản phẩm không còn được bán.");
        em().refresh(product, LockModeType.PESSIMISTIC_WRITE);
        var store = product.getStore(); var category = product.getCategory();
        em().refresh(store); em().refresh(category); em().refresh(store.getOwner());
        if (product.getStatus() != ProductStatus.ACTIVE || store.getStatus() != StoreStatus.ACTIVE
                || !Boolean.TRUE.equals(category.getIsActive()) || !Boolean.TRUE.equals(store.getOwner().getIsActive())
                || !Set.of(UserRole.SELLER, UserRole.ADMIN).contains(store.getOwner().getRole()))
            throw conflict("PRODUCT_UNAVAILABLE", "Sản phẩm hoặc gian hàng không còn được bán.");
        if (product.getStock() == null || product.getStock() < quantity)
            throw conflict("OUT_OF_STOCK", "Sản phẩm “" + product.getName() + "” không đủ tồn kho.");
        if (product.getPrice() == null || product.getPrice().signum() < 0)
            throw conflict("PRODUCT_UNAVAILABLE", "Giá sản phẩm chưa hợp lệ.");
        return item(product, quantity, cartItemId);
    }

    private Item item(Product p, int quantity, Long cartItemId) {
        BigDecimal price = p.getPrice().setScale(2);
        return new Item(cartItemId, p.getId(), p.getName(), p.getImageUrl(), p.getStore().getId(), p.getStore().getName(),
                price, quantity, price.multiply(BigDecimal.valueOf(quantity)));
    }

    private List<CartItem> cartRows(Long buyerId, boolean lock) {
        var query = em().createQuery("select c from Cart c where c.user.id=:buyer", Cart.class).setParameter("buyer", buyerId);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var carts = query.getResultList();
        if (carts.isEmpty()) return List.of();
        var rows = em().createQuery("select i from CartItem i where i.cart.id=:id order by i.product.id", CartItem.class)
                .setParameter("id", carts.getFirst().getId()).setMaxResults(101);
        if (lock) rows.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var result = rows.getResultList();
        if (result.size() > 100) throw MarketplaceException.invalid("Mỗi lần đặt hàng tối đa 100 sản phẩm khác nhau.");
        if (lock) result.forEach(row -> em().refresh(row, LockModeType.PESSIMISTIC_WRITE));
        return result;
    }

    private User buyer(Long id, boolean lock) {
        User user = (lock ? users.lockById(id) : users.findById(id)).orElseThrow(MarketplaceException::missing);
        if (lock) em().refresh(user, LockModeType.PESSIMISTIC_WRITE);
        if (!Boolean.TRUE.equals(user.getIsActive()) || user.getEmailVerifiedAt() == null)
            throw new MarketplaceException(403, "ACCOUNT_UNAVAILABLE", "Vui lòng đăng nhập bằng tài khoản đã xác thực.");
        return user;
    }

    private CheckoutAttempt owned(Long buyerId, String id, boolean lock) {
        CheckoutAttempt row = em().find(CheckoutAttempt.class, id);
        if (row == null || !row.getBuyerUserId().equals(buyerId)) throw MarketplaceException.missing();
        if (lock) em().refresh(row, LockModeType.PESSIMISTIC_WRITE);
        return row;
    }

    private void validate(Request r) {
        if (r == null || r.source() == null || r.addressId() == null || r.addressId() < 1
                || (r.notes() != null && r.notes().length() > 1000)) throw MarketplaceException.invalid("Thông tin đặt hàng không hợp lệ.");
        if (r.source() == Source.BUY_NOW && (r.productId() == null || r.productId() < 1 || r.quantity() == null
                || r.quantity() < 1 || r.quantity() > 1_000_000)) throw MarketplaceException.invalid("Chọn sản phẩm và số lượng hợp lệ.");
        if (r.source() == Source.CART && (r.productId() != null || r.quantity() != null))
            throw MarketplaceException.invalid("Đặt từ giỏ hàng không nhận sản phẩm hoặc số lượng riêng.");
    }
    private MarketplaceException conflict(String code, String message) { return new MarketplaceException(409, code, message); }
    // Resolve only when checkout is used; public catalog/health smoke contexts intentionally omit JPA.
    private EntityManager em() {
        EntityManager manager = entityManagers.getIfAvailable();
        if (manager == null) throw new MarketplaceException(503, "CHECKOUT_UNAVAILABLE", "Dịch vụ đặt hàng chưa sẵn sàng.");
        return manager;
    }
    private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC); }
    private String iso(LocalDateTime time) { return time.atOffset(ZoneOffset.UTC).toString(); }
}
