package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.address.Address;
import com.bonsaimarket.backend.marketplace.ManagementPage;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceAudit;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.marketplace.MarketplaceTransactions;
import com.bonsaimarket.backend.store.Store;
import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Admin quản lý người dùng (12a): tìm, xem, kích hoạt và khóa tài khoản. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class UserAdminService {
    private final EntityManager em;
    private final MarketplaceAccess access;

    public UserAdminService(EntityManager em, MarketplaceAccess access) {
        this.em = em;
        this.access = access;
    }

    public record UserRow(Long id, String name, String email, String phone, String role, boolean active, boolean emailVerified, String createdAt) {
        static UserRow from(User user) {
            return new UserRow(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name(),
                Boolean.TRUE.equals(user.getIsActive()), user.getEmailVerifiedAt() != null, ManagementSupport.iso(user.getCreatedAt()));
        }
    }

    public record StoreInfo(Long id, String name, String status) { }

    public record AddressInfo(Long id, String recipient, String phone, String text, boolean isDefault) { }

    public record OrderInfo(Long id, String code, String status, java.math.BigDecimal total, String storeName, String createdAt) { }

    public record UserDetail(UserRow user, StoreInfo store, List<AddressInfo> addresses, long orderCount, List<OrderInfo> recentOrders) { }

    @Transactional(readOnly = true)
    public ManagementPage<UserRow> list(Long actorId, int page, String search, String role, String active) {
        access.admin(actorId);
        MarketplaceAccess.page(page);
        StringBuilder where = new StringBuilder(" where u.id>0");
        var params = new java.util.LinkedHashMap<String, Object>();
        String like = ManagementSupport.like(search);
        if (like != null) {
            where.append(" and (lower(u.name) like :q or lower(u.email) like :q)");
            params.put("q", like);
        }
        if (role != null && !role.isBlank()) {
            try {
                params.put("role", UserRole.valueOf(role.strip().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw MarketplaceException.invalid("Vai trò không hợp lệ.");
            }
            where.append(" and u.role=:role");
        }
        if (active != null && !active.isBlank()) {
            if (!active.equals("true") && !active.equals("false")) throw MarketplaceException.invalid("Trạng thái tài khoản không hợp lệ.");
            params.put("active", Boolean.valueOf(active));
            where.append(" and u.isActive=:active");
        }
        var count = em.createQuery("select count(u) from User u" + where, Long.class);
        var rows = em.createQuery("select u from User u" + where + " order by u.id desc", User.class);
        params.forEach((key, value) -> {
            count.setParameter(key, value);
            rows.setParameter(key, value);
        });
        long total = count.getSingleResult();
        rows.setFirstResult(page * ManagementSupport.PAGE_SIZE).setMaxResults(ManagementSupport.PAGE_SIZE);
        List<UserRow> content = rows.getResultList().stream().map(UserRow::from).collect(Collectors.toList());
        int pages = (int) ((total + ManagementSupport.PAGE_SIZE - 1) / ManagementSupport.PAGE_SIZE);
        return new ManagementPage<>(content, page, ManagementSupport.PAGE_SIZE, total, pages);
    }

    @Transactional(readOnly = true)
    public UserDetail detail(Long actorId, Long id) {
        access.admin(actorId);
        User user = em.find(User.class, id);
        if (user == null) throw MarketplaceException.missing();
        return detailOf(user);
    }

    public UserRow setActive(Long actorId, Long id, boolean active, String reason) {
        MarketplaceTransactions.requireReadCommitted(em);
        access.admin(actorId);
        if (actorId.equals(id)) throw new MarketplaceException(409, "SELF_CHANGE", "Không thể thay đổi trạng thái tài khoản của chính bạn.");
        User user = access.lock(User.class, id);
        if (Boolean.TRUE.equals(user.getIsActive()) == active) return UserRow.from(user);
        user.setIsActive(active);
        if (!active) user.setAuthVersion(user.getAuthVersion() + 1);
        user.setUpdatedAt(access.now());
        MarketplaceAudit.record(em, access, actorId, active ? "USER_ACTIVATE" : "USER_DEACTIVATE", "USER", id, reason);
        em.flush();
        return UserRow.from(user);
    }

    private UserDetail detailOf(User user) {
        Store store = em.createQuery("select s from Store s where s.owner.id=:id", Store.class)
            .setParameter("id", user.getId()).getResultStream().findFirst().orElse(null);
        StoreInfo storeInfo = store == null ? null : new StoreInfo(store.getId(), store.getName(), store.getStatus() == null ? null : store.getStatus().name());
        List<AddressInfo> addresses = new ArrayList<>();
        for (Address a : em.createQuery("select a from Address a where a.user.id=:id order by a.id", Address.class)
            .setParameter("id", user.getId()).getResultList()) {
            String text = Stream.of(a.getAddressDetail(), a.getWardName(), a.getDistrictName(), a.getProvinceName())
                .filter(part -> part != null && !part.isBlank()).collect(Collectors.joining(", "));
            addresses.add(new AddressInfo(a.getId(), a.getRecipientName(), a.getPhone(), text, Boolean.TRUE.equals(a.getIsDefault())));
        }
        long orderCount = em.createQuery("select count(o) from Order o where o.buyer.id=:id", Long.class)
            .setParameter("id", user.getId()).getSingleResult();
        List<OrderInfo> recent = new ArrayList<>();
        for (Object[] r : em.createQuery("select o.id, o.code, o.status, o.total, s.name, o.createdAt from Order o join o.store s"
                + " where o.buyer.id=:id order by o.createdAt desc, o.id desc", Object[].class)
            .setParameter("id", user.getId()).setMaxResults(10).getResultList()) {
            recent.add(new OrderInfo(((Number) r[0]).longValue(), String.valueOf(r[1]), ((com.bonsaimarket.backend.order.OrderStatus) r[2]).name(),
                new java.math.BigDecimal(r[3].toString()), String.valueOf(r[4]), ManagementSupport.iso((java.time.LocalDateTime) r[5])));
        }
        return new UserDetail(UserRow.from(user), storeInfo, addresses, orderCount, recent);
    }
}
