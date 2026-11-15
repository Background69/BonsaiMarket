package com.bonsaimarket.backend.management;

import com.bonsaimarket.backend.marketplace.*;

import java.util.*;

import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class AdminOverviewService {
    public record Queue(String label, long count, String link, String note) {
    }

    public record Overview(StatisticsService.Overview statistics, long users, long activeUsers, long activeStores,
                           long products, List<Queue> queue, boolean developmentData, long emptyOrders) {
    }

    private final MarketplaceAccess access;
    private final StatisticsService statistics;
    private final JdbcTemplate db;
    private final Environment env;

    public AdminOverviewService(MarketplaceAccess access, StatisticsService statistics, JdbcTemplate db, Environment env) {
        this.access = access;
        this.statistics = statistics;
        this.db = db;
        this.env = env;
    }

    private long count(String sql) {
        return db.queryForObject(sql, Long.class);
    }

    public Overview get(Long actor, String from, String to) {
        access.admin(actor);
        var stats = statistics.admin(actor, from, to);
        var queues = List.of(
                new Queue("Yêu cầu mở gian hàng", count("select count(*) from seller_applications where status='PENDING'"), "/admin/store-applications?status=PENDING", "Đang chờ xét duyệt"),
                new Queue("Báo cáo vi phạm", count("select count(*) from marketplace_reports where status in ('OPEN','IN_REVIEW')"), "/admin/reports?status=OPEN", "Mới gửi hoặc đang xem xét"),
                new Queue("Hồ sơ hoàn tiền", count("select count(*) from order_refunds where status in ('REQUESTED','UNKNOWN','FAILED','AWAITING_EXTERNAL')"), "/admin/refunds", "Cần xét duyệt hoặc đối soát"),
                new Queue("Tranh chấp cần phản hồi", count("select count(*) from support_cases where status in ('OPEN','IN_REVIEW')"), "/admin/disputes", "Hồ sơ hỗ trợ đang mở"));
        boolean dev = env.getProperty("spring.flyway.locations", "").contains("db/dev") || Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equals("dev") || p.startsWith("isolated") || p.endsWith("test"));
        return new Overview(stats, count("select count(*) from users"), count("select count(*) from users where is_active=true"), count("select count(*) from stores s join users u on u.id=s.owner_user_id where s.status='ACTIVE' and u.is_active=true"), count("select count(*) from products"), queues, dev, count("select count(*) from orders o where not exists(select 1 from order_items i where i.order_id=o.id)"));
    }
}
