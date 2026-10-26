package com.bonsaimarket.backend.payment;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.order.Order;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class CodReports {
    private final EntityManager em;private final MarketplaceAccess access;
    public CodReports(EntityManager em,MarketplaceAccess access){this.em=em;this.access=access;}
    public Object view(Long actor,Long id,boolean admin){
        if(admin)access.admin(actor);else access.seller(actor);var o=em.find(Order.class,id);
        if(o==null||!admin&&!o.getStore().getOwner().getId().equals(actor))throw MarketplaceException.missing();
        return Map.of("reports",em.createQuery("select r from CodProviderReport r where r.orderId=:id order by r.reportedAt desc",CodProviderReport.class).setParameter("id",id).setMaxResults(100).getResultList(),"settlementVerification","AUTHORITATIVE_STATEMENT_REQUIRED","collectionVerification","NO_INDEPENDENT_COLLECTION_CONTRACT","refundVerification","EXTERNAL_PROVIDER_REQUIRED");
    }
}
