package com.bonsaimarket.backend.marketplace;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import com.bonsaimarket.backend.user.*;
import jakarta.persistence.*;
import java.time.*;
import org.springframework.stereotype.Component;

/** Shared actor and lock rules for marketplace services; ownership always comes from the session. */
@Component
public class MarketplaceAccess {
    private final EntityManager em;
    private final Clock clock;
    public MarketplaceAccess(EntityManager em, Clock clock) { this.em=em; this.clock=clock; }
    public User actor(Long id) {
        User user=em.find(User.class,id);
        if(user==null || !Boolean.TRUE.equals(user.getIsActive())) throw new MarketplaceException(403,"FORBIDDEN","Tài khoản không khả dụng.");
        return user;
    }
    public User admin(Long id) {
        User user=actor(id);
        if(user.getRole()!=UserRole.ADMIN) throw new MarketplaceException(403,"FORBIDDEN","Chỉ quản trị viên được thực hiện thao tác này.");
        return user;
    }
    public User seller(Long id) {
        User user=actor(id);
        if(user.getRole()!=UserRole.SELLER && user.getRole()!=UserRole.ADMIN) throw new MarketplaceException(403,"FORBIDDEN","Cần quyền người bán.");
        return user;
    }
    public <T> T lock(Class<T> type, Long id) {
        T row=em.find(type,id);
        if(row==null) throw MarketplaceException.missing();
        em.refresh(row,LockModeType.PESSIMISTIC_WRITE);
        return row;
    }
    public LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
    public static void revision(long actual, long expected) {
        if(actual!=expected) throw new MarketplaceException(409,"DATA_CONFLICT","Dữ liệu vừa thay đổi. Tải lại để xem trước khi gửi lại.");
    }
    public static int page(int page) {
        if(page<0 || page>10000) throw MarketplaceException.invalid("Trang phải từ 0 đến 10.000.");
        return page;
    }
    public static String text(String text) { return text==null ? "" : text.strip(); }
}
