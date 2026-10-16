package com.bonsaimarket.backend.inventory;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.marketplace.MarketplaceTransactions;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.order.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Direct debit model: stock is AVAILABLE unsold stock; no reservations or timeout worker. */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class InventoryService {
    public static final int MAX_QUANTITY=1_000_000;
    private final EntityManager em;
    private final ProductRepository products;
    private final UserRepository users;
    private final InventoryDebitRepository debits;
    private final Clock clock;
    public InventoryService(EntityManager em,ProductRepository products,UserRepository users,InventoryDebitRepository debits,Clock clock) {this.em=em;this.products=products;this.users=users;this.debits=debits;this.clock=clock;}
    public record StockView(Long productId,String name,Integer stock,String status,Long storeId,String imageUrl) {
        static StockView from(Product p) {return new StockView(p.getId(),p.getName(),p.getStock(),p.getStatus().name(),p.getStore().getId(),p.getImageUrl());}
    }
    public record StockPage(List<StockView> content,int page,int size,long totalElements,int totalPages) { }
    @Transactional(readOnly=true)
    public StockPage list(Long actorId,int page,int size) {
        var actor=actor(actorId); if(page<0 || page>10000 || size<1 || size>50) throw MarketplaceException.invalid("Trang hoặc số sản phẩm mỗi trang không hợp lệ.");
        Specification<Product> spec=(root,q,cb)->actor.getRole()==UserRole.ADMIN ? cb.conjunction() : cb.equal(root.get("store").get("owner").get("id"),actorId);
        var result=products.findAll(spec,PageRequest.of(page,size,Sort.by("id")));
        return new StockPage(result.map(StockView::from).getContent(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    public StockView adjust(Long actorId,Long productId,int stock,int expectedStock) {
        MarketplaceTransactions.requireReadCommitted(em);
        if(stock<0 || stock>MAX_QUANTITY || expectedStock<0) throw MarketplaceException.invalid("Số lượng tồn kho phải từ 0 đến 1.000.000.");
        var actor=actor(actorId);var p=lockProduct(productId);
        if(actor.getRole()!=UserRole.ADMIN && !p.getStore().getOwner().getId().equals(actorId)) throw MarketplaceException.missing();
        if(p.getStock()==null || p.getStock()!=expectedStock) throw conflict("STOCK_CHANGED","Tồn kho vừa thay đổi. Tải lại trước khi cập nhật.");
        p.setStock(stock);p.setUpdatedAt(now());return StockView.from(p);
    }

    /** Order service must create/confirm order+items and call this in ONE database transaction.
     * No HTTP endpoint: order ownership, totals, payment eligibility belong to the caller. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void debitConfirmedOrder(Long orderId) {
        var order=lockOrder(orderId);
        if(order.getStatus()!=OrderStatus.CONFIRMED) throw conflict("ORDER_STATE_INVALID","Chỉ trừ tồn khi đơn được xác nhận.");
        var lines=em.createQuery("select i from OrderItem i where i.order.id=:id order by i.product.id",OrderItem.class)
            .setParameter("id",orderId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if(lines.isEmpty() || lines.size()>100) throw MarketplaceException.invalid("Đơn phải có từ 1 đến 100 dòng sản phẩm.");
        var quantities=new TreeMap<Long,Integer>();
        for(var line:lines) {
            em.refresh(line,LockModeType.PESSIMISTIC_WRITE);
            long productId=line.getProduct().getId();int qty=line.getQuantity();
            if(qty<1 || qty>MAX_QUANTITY) throw MarketplaceException.invalid("Số lượng đặt hàng không hợp lệ.");
            long combined=(long)quantities.getOrDefault(productId,0)+qty;
            if(combined>MAX_QUANTITY) throw MarketplaceException.invalid("Tổng số lượng đặt hàng vượt giới hạn.");
            quantities.put(productId,(int)combined);
        }
        var previous=currentDebits(orderId);
        if(!previous.isEmpty()) {
            var existing=new TreeMap<Long,Integer>();previous.forEach(d->existing.put(d.getProduct().getId(),d.getQuantity()));
            if(previous.stream().anyMatch(InventoryDebit::isRestored) || !existing.equals(quantities)) throw conflict("INVENTORY_RETRY_CONFLICT","Đơn đã trả tồn hoặc nội dung đơn đã thay đổi.");
            return;
        }
        // A stable ascending product ID order is shared by debit and restore.
        for(var line:quantities.entrySet()) {
            var p=lockProduct(line.getKey());
            if(!p.getStore().getId().equals(order.getStore().getId()) || p.getStatus()!=ProductStatus.ACTIVE
                || p.getStore().getStatus()!=StoreStatus.ACTIVE || !Boolean.TRUE.equals(p.getCategory().getIsActive()))
                throw conflict("PRODUCT_UNAVAILABLE","Sản phẩm không còn được bán trong gian hàng này.");
            if(p.getStock()==null || p.getStock()<line.getValue()) throw conflict("OUT_OF_STOCK","Sản phẩm không đủ số lượng tồn kho.");
            p.setStock(p.getStock()-line.getValue());p.setUpdatedAt(now());
            var debit=new InventoryDebit();debit.setOrder(order);debit.setProduct(p);debit.setQuantity(line.getValue());debit.setCreatedAt(now());debit.setUpdatedAt(now());debits.save(debit);
        }
        em.flush();
    }
    /** Caller sets CANCELLED and invokes this in the same transaction. Repeated cancellation is a no-op. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void restoreCancelledOrder(Long orderId) {
        var order=lockOrder(orderId);
        if(order.getStatus()!=OrderStatus.CANCELLED) throw conflict("ORDER_STATE_INVALID","Chỉ trả tồn cho đơn đã hủy.");
        for(var debit:currentDebits(orderId)) {
            if(debit.isRestored()) continue;
            var p=lockProduct(debit.getProduct().getId());
            long restored=(long)p.getStock()+debit.getQuantity();
            if(restored>Integer.MAX_VALUE) throw conflict("STOCK_OVERFLOW","Số lượng tồn kho vượt giới hạn.");
            p.setStock((int)restored);p.setUpdatedAt(now());debit.setRestored(true);debit.setUpdatedAt(now());
        }
        em.flush();
    }
    private Order lockOrder(Long id) {
        MarketplaceTransactions.requireReadCommitted(em);
        // Flush the caller's newly created items/status before refreshing from the locked row.
        em.flush(); var order=em.find(Order.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(order==null) throw MarketplaceException.missing();em.refresh(order,LockModeType.PESSIMISTIC_WRITE);return order;
    }
    private List<InventoryDebit> currentDebits(Long orderId) {
        var rows=debits.findByOrderIdOrderByProductIdAsc(orderId);
        rows.forEach(row->em.refresh(row,LockModeType.PESSIMISTIC_WRITE));return rows;
    }
    private Product lockProduct(Long id) {
        var product=em.find(Product.class,id);if(product==null) throw MarketplaceException.missing();
        // Refresh under the lock even if the caller previously loaded pricing/catalog entities.
        em.refresh(product,LockModeType.PESSIMISTIC_WRITE);return product;
    }
    private User actor(Long id) {
        var user=users.findById(id).orElseThrow(MarketplaceException::missing);
        if(!Boolean.TRUE.equals(user.getIsActive()) || (user.getRole()!=UserRole.SELLER && user.getRole()!=UserRole.ADMIN)) throw new MarketplaceException(403,"FORBIDDEN","Bạn không có quyền quản lý tồn kho.");return user;
    }
    private LocalDateTime now() {return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    private MarketplaceException conflict(String code,String message) {return new MarketplaceException(409,code,message);}
}
