package com.bonsaimarket.backend.report;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceAudit;
import com.bonsaimarket.backend.marketplace.ManagementPage;

import com.bonsaimarket.backend.user.User;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.*;
import static com.bonsaimarket.backend.report.ReportDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class ReportService {
    private final EntityManager em;private final MarketplaceAccess access;private final ReportRepository reports;
    public ReportService(EntityManager em,MarketplaceAccess access,ReportRepository reports) {this.em=em;this.access=access;this.reports=reports;}
    public ReportReceipt create(Long actor,ReportWrite body) {
        access.actor(actor);var user=access.lock(User.class,actor);
        if(body.targetType().equals("PRODUCT")) {
            var p=em.find(Product.class,body.targetId());
            if(p==null || p.getStatus()!=ProductStatus.ACTIVE || p.getStore().getStatus()!=StoreStatus.ACTIVE || !Boolean.TRUE.equals(p.getCategory().getIsActive())) throw MarketplaceException.missing();
        } else if(body.targetType().equals("STORE")) {
            var s=em.find(Store.class,body.targetId());if(s==null || s.getStatus()!=StoreStatus.ACTIVE) throw MarketplaceException.missing();
        } else throw MarketplaceException.invalid("Đối tượng không hợp lệ.");
        if(reports.existsByReporterIdAndTargetTypeAndTargetId(actor,body.targetType(),body.targetId())) throw new MarketplaceException(409,"REPORT_EXISTS","Bạn đã gửi báo cáo cho đối tượng này.");
        if(reports.countByReporterIdAndCreatedAtAfter(actor,access.now().minusHours(1))>=5) throw new MarketplaceException(429,"REPORT_RATE_LIMIT","Bạn đã gửi 5 báo cáo trong một giờ. Vui lòng thử lại sau.");
        if(body.reason().equals("OTHER") && body.description().isBlank()) throw MarketplaceException.invalid("Vui lòng mô tả lý do khác.");
        var r=new MarketplaceReport();r.setReporter(user);r.setTargetType(body.targetType());r.setTargetId(body.targetId());r.setReason(body.reason());
        r.setDescription(body.description().strip());r.setStatus("OPEN");r.setCreatedAt(access.now());reports.saveAndFlush(r);return new ReportReceipt(r.getId(),r.getStatus());
    }
    @Transactional(readOnly=true) public ManagementPage<ReportView> list(Long actor,String status,int page) {
        access.admin(actor);if(!java.util.Set.of("OPEN","IN_REVIEW","RESOLVED","DISMISSED").contains(status)) throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        return ManagementPage.of(reports.findByStatus(status,PageRequest.of(MarketplaceAccess.page(page),20,Sort.by("createdAt").descending().and(Sort.by("id")))).map(this::view));
    }
    public ReportView decide(Long actor,Long id,ReportDecision body) {
        access.admin(actor);var r=access.lock(MarketplaceReport.class,id);MarketplaceAccess.revision(r.getRevision(),body.revision());
        if(!(r.getStatus().equals("OPEN") && body.status().equals("IN_REVIEW") || r.getStatus().equals("IN_REVIEW") && java.util.Set.of("RESOLVED","DISMISSED").contains(body.status())))
            throw new MarketplaceException(409,"REPORT_STATE_INVALID","Báo cáo cần được tiếp nhận trước khi kết thúc; báo cáo đã đóng không thể sửa.");
        r.setStatus(body.status());r.setResolution(body.reason().strip());r.setProcessedBy(actor);r.setProcessedAt(access.now());em.flush();
        MarketplaceAudit.record(em,access,actor,"REPORT_"+body.status(),"REPORT",id,body.reason());return view(r);
    }
    @Transactional(readOnly=true)
    public java.util.List<ReportHistoryView> history(Long actor,Long id) {
        access.admin(actor);
        if(!reports.existsById(id)) throw MarketplaceException.missing();
        return em.createQuery("select a from MarketplaceAudit a where a.targetType='REPORT' and a.targetId=:id order by a.createdAt,a.id",MarketplaceAudit.class)
            .setParameter("id",id).setMaxResults(100).getResultStream()
            .map(a->new ReportHistoryView(a.getId(),a.getAction(),a.getReason(),a.getCreatedAt())).toList();
    }
    private ReportView view(MarketplaceReport r) {return new ReportView(r.getId(),r.getTargetType(),r.getTargetId(),r.getReason(),r.getDescription(),r.getStatus(),r.getResolution(),r.getCreatedAt(),r.getProcessedAt(),r.getRevision());}
}
