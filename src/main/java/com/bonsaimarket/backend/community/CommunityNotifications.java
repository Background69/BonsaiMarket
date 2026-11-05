package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import com.bonsaimarket.backend.notification.NotificationOutbox;
import com.bonsaimarket.backend.user.User;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class CommunityNotifications {
    private final EntityManager em;private final MarketplaceAccess access;
    public CommunityNotifications(EntityManager em,MarketplaceAccess access){this.em=em;this.access=access;}
    public void enqueue(long post,Long actor,long recipient,String event,String operation){
        if(actor.equals(recipient))return;var user=em.find(User.class,recipient);if(user==null||!Boolean.TRUE.equals(user.getIsActive()))return;
        String message=switch(event){case "COMMUNITY_COMMENTED"->"Bài viết của bạn có bình luận mới.";case "COMMUNITY_ACCEPTED"->"Tác giả đã chọn bình luận của bạn là giải pháp hữu ích. Đây là lựa chọn của tác giả, không phải chứng nhận chuyên môn.";case "COMMUNITY_MODERATED"->"Nội dung của bạn đã được quản trị viên kiểm duyệt. Nội dung có thể không còn công khai. Liên hệ quản trị nếu cần giải thích thêm.";default->throw new IllegalArgumentException("Unknown community event");};
        NotificationOutbox.enqueue(em,access,null,event,user.getEmail(),"COMMUNITY:"+post+":"+operation+":USER:"+recipient,"BonsaiMarket — Cập nhật cộng đồng",message+"\nBài viết: /community/posts/"+post);
    }
}
