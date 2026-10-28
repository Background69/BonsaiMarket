package com.bonsaimarket.backend.marketplace;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import org.hibernate.Session;

/** Kiểm tra mức cô lập cả khi REQUIRED/MANDATORY tham gia giao dịch của bên gọi. */
public final class MarketplaceTransactions {
    private MarketplaceTransactions() { }
    public static void requireReadCommitted(EntityManager em) {
        int isolation=em.unwrap(Session.class).doReturningWork(Connection::getTransactionIsolation);
        if(isolation!=Connection.TRANSACTION_READ_COMMITTED) throw new MarketplaceException(409,"TRANSACTION_ISOLATION_REQUIRED",
            "Thao tác cần transaction READ_COMMITTED. Vui lòng kiểm tra cấu hình tích hợp.");
    }
}
