package com.bonsaimarket.backend.seller.application;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.marketplace.MarketplaceAudit;
import com.bonsaimarket.backend.marketplace.ManagementPage;

import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import com.bonsaimarket.backend.marketplace.MarketplaceException;
import jakarta.persistence.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.bonsaimarket.backend.seller.application.SellerApplicationDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class SellerApplicationService {
    private final EntityManager em; private final ApplicationRepository applications; private final MarketplaceAccess access;
    public SellerApplicationService(EntityManager em,ApplicationRepository applications,MarketplaceAccess access) {this.em=em;this.applications=applications;this.access=access;}
    private Store store(Long user) {return em.createQuery("select s from Store s where s.owner.id=:id",Store.class).setParameter("id",user).getResultStream().findFirst().orElse(null);}
    @Transactional(readOnly=true) public ApplicationView current(Long actor) {
        var user=access.actor(actor);var s=store(actor);
        if(s!=null) return new ApplicationView(null,"APPROVED",s.getName(),s.getDescription(),s.getPhone(),s.getAddress(),"",user.getName(),s.getCreatedAt(),null,0,s.getId());
        return applications.findByApplicantId(actor).map(this::view).orElse(null);
    }
    public ApplicationView submit(Long actor,ApplicationWrite body) {
        access.actor(actor);var user=access.lock(User.class,actor);
        if(user.getRole()!=UserRole.CUSTOMER || store(actor)!=null) throw conflict("STORE_ALREADY_EXISTS","Tài khoản đã có gian hàng hoặc không thuộc nhóm đăng ký.");
        var app=applications.findByApplicantId(actor).orElse(null);
        if(app!=null) {
            app=access.lock(SellerApplication.class,app.getId());MarketplaceAccess.revision(app.getRevision(),body.revision());
            if(!app.getStatus().equals("REJECTED")) throw conflict("APPLICATION_EXISTS","Yêu cầu đã được gửi. Vui lòng xem trạng thái.");
        } else {if(body.revision()!=0) throw conflict("DATA_CONFLICT","Vui lòng tải lại hồ sơ đăng ký.");app=new SellerApplication();app.setApplicant(user);}
        app.setName(body.name().strip());app.setDescription(body.description().strip());app.setPhone(body.phone().strip());app.setAddress(body.address().strip());
        app.setStatus("PENDING");app.setReason("");app.setSubmittedAt(access.now());app.setProcessedAt(null);app.setProcessedBy(null);applications.saveAndFlush(app);
        MarketplaceAudit.record(em,access,actor,"APPLICATION_SUBMITTED","APPLICATION",app.getId(),"");return view(app);
    }
    @Transactional(readOnly=true) public ManagementPage<ApplicationView> list(Long actor,String status,int page) {
        access.admin(actor);if(!java.util.Set.of("PENDING","APPROVED","REJECTED").contains(status)) throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        return ManagementPage.of(applications.findByStatus(status,PageRequest.of(MarketplaceAccess.page(page),20,Sort.by("submittedAt").descending().and(Sort.by("id")))).map(this::view));
    }
    public ApplicationView decide(Long actor,Long id,Decision body) {
        access.admin(actor);var initial=applications.findById(id).orElseThrow(MarketplaceException::missing);
        // Khóa user rồi hồ sơ như lúc gửi; admin đồng thời không được tạo hai gian hàng.
        var user=access.lock(User.class,initial.getApplicant().getId());var app=access.lock(SellerApplication.class,id);
        MarketplaceAccess.revision(app.getRevision(),body.revision());
        if(!app.getStatus().equals("PENDING")) throw conflict("APPLICATION_DECIDED","Yêu cầu đã được xử lý.");
        if(actor.equals(user.getId())) throw new MarketplaceException(403,"SELF_APPROVAL_FORBIDDEN","Không được tự xét duyệt hồ sơ.");
        if(body.status().equals("REJECTED") && body.reason().isBlank()) throw MarketplaceException.invalid("Cần lý do từ chối.");
        if(body.status().equals("APPROVED")) {
            if(!Boolean.TRUE.equals(user.getIsActive()) || user.getRole()!=UserRole.CUSTOMER || store(user.getId())!=null) throw conflict("APPLICANT_CHANGED","Tài khoản hoặc gian hàng đã thay đổi. Vui lòng kiểm tra lại.");
            var s=new Store();s.setOwner(user);s.setName(app.getName());s.setDescription(app.getDescription());s.setPhone(app.getPhone());s.setAddress(app.getAddress());
            s.setStatus(StoreStatus.ACTIVE);s.setCreatedAt(access.now());s.setUpdatedAt(access.now());em.persist(s);user.setRole(UserRole.SELLER);user.setUpdatedAt(access.now());
        }
        app.setStatus(body.status());app.setReason(body.reason().strip());app.setProcessedBy(actor);app.setProcessedAt(access.now());em.flush();
        MarketplaceAudit.record(em,access,actor,"APPLICATION_"+body.status(),"APPLICATION",id,body.reason());return view(app);
    }
    private ApplicationView view(SellerApplication a) {var s=store(a.getApplicant().getId());return new ApplicationView(a.getId(),a.getStatus(),a.getName(),a.getDescription(),a.getPhone(),a.getAddress(),a.getReason(),a.getApplicant().getName(),a.getSubmittedAt(),a.getProcessedAt(),a.getRevision(),s==null?null:s.getId());}
    private MarketplaceException conflict(String code,String message) {return new MarketplaceException(409,code,message);}
}
