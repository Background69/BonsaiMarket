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
    private final EntityManager em;private final MarketplaceAccess access;private final ReportRepository reports;private final org.springframework.jdbc.core.JdbcTemplate db;
    public ReportService(EntityManager em,MarketplaceAccess access,ReportRepository reports,org.springframework.jdbc.core.JdbcTemplate db) {this.em=em;this.access=access;this.reports=reports;this.db=db;}
    public ReportReceipt create(Long actor,ReportWrite body) {
        access.actor(actor);var user=access.lock(User.class,actor);
        if(body.targetType().equals("USER")) {
            if(!"ORDER".equals(body.contextType())||body.contextId()==null||body.requestKey()==null||!body.requestKey().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw MarketplaceException.invalid("Cần đơn hàng hợp lệ và mã yêu cầu để báo cáo khách hàng.");
            var context=customerContext(actor,body.contextId());
            if(!context.eligible()||!context.customerId().equals(body.targetId())||actor.equals(body.targetId()))throw MarketplaceException.missing();
            var previous=em.createQuery("select r from MarketplaceReport r where r.reporter.id=:actor and r.requestKey=:key",MarketplaceReport.class).setParameter("actor",actor).setParameter("key",body.requestKey()).getResultList();
            if(!previous.isEmpty()){
                var r=previous.getFirst();
                if(!fingerprint(body).equals(r.getRequestFingerprint()))throw new MarketplaceException(409,"REPORT_REPLAY_CONFLICT","Mã yêu cầu đã dùng với nội dung khác. Hãy đối chiếu báo cáo đã gửi.");
                return new ReportReceipt(r.getId(),r.getStatus());
            }
        } else if(body.contextType()!=null||body.contextId()!=null||body.requestKey()!=null)throw MarketplaceException.invalid("Ngữ cảnh khách hàng chỉ dùng cho báo cáo USER.");
        else if(body.targetType().equals("PRODUCT")) {
            var p=em.find(Product.class,body.targetId());
            if(p==null || p.getStatus()!=ProductStatus.ACTIVE || p.getStore().getStatus()!=StoreStatus.ACTIVE || !Boolean.TRUE.equals(p.getCategory().getIsActive())) throw MarketplaceException.missing();
        } else if(body.targetType().equals("STORE")) {
            var s=em.find(Store.class,body.targetId());if(s==null || s.getStatus()!=StoreStatus.ACTIVE) throw MarketplaceException.missing();
        } else if(body.targetType().equals("COMMUNITY_POST")) {
            if(db.queryForObject("select count(*) from community_posts where id=? and status='PUBLIC'",Long.class,body.targetId())==0)throw MarketplaceException.missing();
        } else if(body.targetType().equals("COMMUNITY_COMMENT")) {
            if(db.queryForObject("select count(*) from community_comments c join community_posts p on p.id=c.post_id where c.id=? and c.status='PUBLIC' and p.status='PUBLIC'",Long.class,body.targetId())==0)throw MarketplaceException.missing();
        } else throw MarketplaceException.invalid("Đối tượng không hợp lệ.");
        if(reports.existsByReporterIdAndTargetTypeAndTargetId(actor,body.targetType(),body.targetId())) throw new MarketplaceException(409,"REPORT_EXISTS","Bạn đã gửi báo cáo cho đối tượng này.");
        if(reports.countByReporterIdAndCreatedAtAfter(actor,access.now().minusHours(1))>=5) throw new MarketplaceException(429,"REPORT_RATE_LIMIT","Bạn đã gửi 5 báo cáo trong một giờ. Vui lòng thử lại sau.");
        if(body.reason().equals("OTHER") && body.description().isBlank()) throw MarketplaceException.invalid("Vui lòng mô tả lý do khác.");
        var r=new MarketplaceReport();r.setReporter(user);r.setTargetType(body.targetType());r.setTargetId(body.targetId());r.setReason(body.reason());
        r.setDescription(body.description().strip());r.setStatus("OPEN");r.setCreatedAt(access.now());
        if(body.targetType().equals("USER")){r.setCustomerUserId(body.targetId());r.setContextOrderId(body.contextId());r.setRequestKey(body.requestKey());r.setRequestFingerprint(fingerprint(body));}
        reports.saveAndFlush(r);
        if(body.targetType().startsWith("COMMUNITY_")){
            String table=body.targetType().equals("COMMUNITY_POST")?"community_posts":"community_comments";
            var target=db.queryForMap("select revision,body from "+table+" where id=?",body.targetId());
            String hash;try{hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(target.get("body").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
            MarketplaceAudit.record(em,access,actor,"COMMUNITY_REPORTED","REPORT",r.getId(),body.targetType()+":"+body.targetId()+"; revision="+target.get("revision")+"; bodySHA256="+hash);
        }else com.bonsaimarket.backend.notification.InAppNotifications.report(em,access,actor,r.getId());
        if(body.targetType().equals("USER")){
            MarketplaceAudit.record(em,access,actor,"REPORT_CREATED","REPORT",r.getId(),"Đã gửi báo cáo khách hàng trong ngữ cảnh đơn hàng.");
            com.bonsaimarket.backend.notification.InAppNotifications.reportSubmitted(em,access,actor,r.getId());
        }
        return new ReportReceipt(r.getId(),r.getStatus());
    }
    @Transactional(readOnly=true) public ManagementPage<ReportView> list(Long actor,String status,int page) {
        access.admin(actor);if(!java.util.Set.of("OPEN","IN_REVIEW","RESOLVED","DISMISSED").contains(status)) throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        return ManagementPage.of(reports.findByStatus(status,PageRequest.of(MarketplaceAccess.page(page),20,Sort.by("createdAt").descending().and(Sort.by("id")))).map(this::view));
    }
    @Transactional(readOnly=true) public ManagementPage<ReportView> community(Long actor,String status,int page) {
        access.admin(actor);if(!java.util.Set.of("OPEN","IN_REVIEW","RESOLVED","DISMISSED").contains(status))throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        return ManagementPage.of(reports.findByStatusAndTargetTypeIn(status,java.util.List.of("COMMUNITY_POST","COMMUNITY_COMMENT"),PageRequest.of(MarketplaceAccess.page(page),20,Sort.by("createdAt").descending().and(Sort.by("id")))).map(this::view));
    }
    public ReportView decide(Long actor,Long id,ReportDecision body) {
        access.admin(actor);var r=access.lock(MarketplaceReport.class,id);MarketplaceAccess.revision(r.getRevision(),body.revision());
        if(!(r.getStatus().equals("OPEN") && body.status().equals("IN_REVIEW") || r.getStatus().equals("IN_REVIEW") && java.util.Set.of("RESOLVED","DISMISSED").contains(body.status())))
            throw new MarketplaceException(409,"REPORT_STATE_INVALID","Báo cáo cần được tiếp nhận trước khi kết thúc; báo cáo đã đóng không thể sửa.");
        String before=r.getStatus();r.setStatus(body.status());r.setResolution(body.reason().strip());r.setProcessedBy(actor);r.setProcessedAt(access.now());em.flush();
        MarketplaceAudit.transition(em,access,actor,"REPORT_"+body.status(),"REPORT",id,body.reason(),before,body.status());return view(r);
    }
    @Transactional(readOnly=true)
    public java.util.List<ReportHistoryView> history(Long actor,Long id) {
        access.admin(actor);
        if(!reports.existsById(id)) throw MarketplaceException.missing();
        return em.createQuery("select a from MarketplaceAudit a where a.targetType='REPORT' and a.targetId=:id order by a.createdAt,a.id",MarketplaceAudit.class)
            .setParameter("id",id).setMaxResults(100).getResultStream()
            .map(a->new ReportHistoryView(a.getId(),a.getAction(),com.bonsaimarket.backend.marketplace.AuditPresentation.reason(a.getAction(),a.getReason()),a.getCreatedAt())).toList();
    }
    @Transactional(readOnly=true) public ReportView detail(Long actor,Long id){access.admin(actor);return view(reports.findById(id).orElseThrow(MarketplaceException::missing));}
    @Transactional(readOnly=true) public ManagementPage<ReportView> filtered(Long actor,String status,int page,String targetType,Long targetId){
        if(targetType==null||targetType.isBlank())return list(actor,status,page);access.admin(actor);MarketplaceAccess.page(page);
        if(!java.util.Set.of("PRODUCT","STORE","USER","COMMUNITY_POST","COMMUNITY_COMMENT").contains(targetType)||targetId!=null&&targetId<1)throw MarketplaceException.invalid("Đối tượng báo cáo không hợp lệ.");
        if(!java.util.Set.of("OPEN","IN_REVIEW","RESOLVED","DISMISSED").contains(status))throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        String where=" where r.targetType=:type and r.status=:status"+(targetId==null?"":" and r.targetId=:id");
        var count=em.createQuery("select count(r) from MarketplaceReport r"+where,Long.class).setParameter("type",targetType).setParameter("status",status);
        var query=em.createQuery("select r from MarketplaceReport r"+where+" order by r.createdAt desc,r.id desc",MarketplaceReport.class).setParameter("type",targetType).setParameter("status",status);
        if(targetId!=null){count.setParameter("id",targetId);query.setParameter("id",targetId);}
        long total=count.getSingleResult();var rows=query.setFirstResult(page*20).setMaxResults(20).getResultStream().map(this::view).toList();return new ManagementPage<>(rows,page,20,total,(int)((total+19)/20));
    }
    /** Only a SELLER owning this order may discover/report its CUSTOMER. No arbitrary account lookup. */
    @Transactional(readOnly=true) public CustomerContext customerContext(Long actor,Long orderId){
        if(access.actor(actor).getRole()!=com.bonsaimarket.backend.user.UserRole.SELLER)throw new MarketplaceException(403,"FORBIDDEN","Chỉ người bán của đơn hàng được báo cáo khách hàng.");
        var rows=db.queryForList("select u.id,u.name,u.role,o.code from orders o join stores s on s.id=o.store_id join users u on u.id=o.buyer_user_id where o.id=? and s.owner_user_id=?",orderId,actor);
        if(rows.isEmpty())throw MarketplaceException.missing();var row=rows.getFirst();long customer=((Number)row.get("id")).longValue();
        return new CustomerContext(customer,row.get("name").toString(),orderId,row.get("code").toString(),"CUSTOMER".equals(row.get("role"))&&!actor.equals(customer));
    }
    private String fingerprint(ReportWrite body){
        // Length-delimited fields prevent concatenation ambiguities. Whitespace normalization matches persistence.
        String canonical=java.util.stream.Stream.of(body.targetType(),body.targetId().toString(),body.reason(),body.description().strip(),body.contextType(),body.contextId().toString()).map(s->s.length()+":"+s).collect(java.util.stream.Collectors.joining());
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private ReportView view(MarketplaceReport r) {
        CustomerContext context=null;
        if(r.getContextOrderId()!=null){var rows=db.queryForList("select u.name,o.code from users u join orders o on o.id=? where u.id=?",r.getContextOrderId(),r.getCustomerUserId());if(!rows.isEmpty())context=new CustomerContext(r.getCustomerUserId(),rows.getFirst().get("name").toString(),r.getContextOrderId(),rows.getFirst().get("code").toString(),true);}
        return new ReportView(r.getId(),r.getTargetType(),r.getTargetId(),r.getReason(),r.getDescription(),r.getStatus(),r.getResolution(),r.getCreatedAt(),r.getProcessedAt(),r.getRevision(),r.getReporter().getId(),r.getReporter().getName(),context);
    }
}
