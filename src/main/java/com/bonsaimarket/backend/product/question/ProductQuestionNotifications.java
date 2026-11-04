package com.bonsaimarket.backend.product.question;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.notification.NotificationOutbox;
import com.bonsaimarket.backend.product.Product;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class ProductQuestionNotifications {
    private final EntityManager em;
    private final MarketplaceAccess access;
    public ProductQuestionNotifications(EntityManager em, MarketplaceAccess access) { this.em=em; this.access=access; }

    public void asked(Product product, long questionId) {
        var seller = product.getStore().getOwner();
        NotificationOutbox.enqueue(em,access,null,"PRODUCT_QUESTION_ASKED",seller.getEmail(),
            "QUESTION:"+questionId+":ASKED:SELLER:"+seller.getId(),"BonsaiMarket — Câu hỏi sản phẩm mới",
            "Có câu hỏi mới về sản phẩm: "+product.getName()+".\nMở Kênh người bán > Hỏi đáp sản phẩm để xem và trả lời."
                +"\nĐường dẫn: /products/"+product.getId()+"?question="+questionId+"#product-question-"+questionId+"\nBonsaiMarket");
    }

    public void response(Product product,long questionId,Long actor,java.util.Collection<Long> recipients,String event,String operation) {
        for(Long id:new java.util.LinkedHashSet<>(recipients)) {
            if(id.equals(actor))continue;
            var recipient=em.find(com.bonsaimarket.backend.user.User.class,id);
            if(recipient==null||!Boolean.TRUE.equals(recipient.getIsActive()))continue;
            NotificationOutbox.enqueue(em,access,null,event,recipient.getEmail(),"QUESTION:"+questionId+":"+operation+":"+id,
                "BonsaiMarket — Cập nhật hỏi đáp sản phẩm",
                "Hỏi đáp sản phẩm "+product.getName()+" có cập nhật.\nXem trao đổi công khai tại /products/"+product.getId()+"?question="+questionId+"#product-question-"+questionId+"\nKhông phải tin nhắn riêng tư.");
        }
    }
}
