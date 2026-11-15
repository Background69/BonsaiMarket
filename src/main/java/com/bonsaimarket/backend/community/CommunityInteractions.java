package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.*;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
import java.math.BigDecimal;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CommunityInteractions {
    private final CommunityService posts;private final ContentAccess guard;private final JdbcTemplate db;private final EntityManager em;private final StoreLocations eligibility;private final CommunityNotifications notifications;
    public CommunityInteractions(CommunityService posts,ContentAccess guard,JdbcTemplate db,EntityManager em,StoreLocations eligibility,CommunityNotifications notifications){this.posts=posts;this.guard=guard;this.db=db;this.em=em;this.eligibility=eligibility;this.notifications=notifications;}
    public record ProductLink(long id,String name,BigDecimal price,String imageUrl,long storeId,String storeName) {}
    public record Activity(long likeCount,boolean liked,boolean bookmarked,List<ProductLink> products,CommunityService.Comment acceptedAnswer) {}
    public record AnswerWrite(Long commentId,Long revision) {}
    public record StatusWrite(String status,Long revision) {}
    public record LinksWrite(List<Long> productIds,Long revision) {}
    public record NotificationView(String id,String eventType,String body,java.time.LocalDateTime createdAt,String status) {}
    @Transactional(readOnly=true) public Activity activity(long postId,Long actor){
        var p=posts.rawPost(postId);if(actor!=null)guard.access.actor(actor);
        return new Activity(p.likeCount(),actor!=null&&count("community_likes",actor,postId)>0,actor!=null&&count("community_bookmarks",actor,postId)>0,linkedProducts(p),p.acceptedCommentId()==null?null:posts.commentRow(p.acceptedCommentId()));
    }
    private long count(String table,long actor,long post){return db.queryForObject("select count(*) from "+table+" where user_id=? and post_id=?",Long.class,actor,post);}
    public Activity reaction(Long actor,long post,String kind,boolean enabled){
        guard.writer(actor);posts.lockPost(post);String table=switch(kind){case "like"->"community_likes";case "bookmark"->"community_bookmarks";default->throw MarketplaceException.invalid("Tương tác không hợp lệ.");};
        if(enabled&&count(table,actor,post)==0)db.update("insert into "+table+"(user_id,post_id,created_at) values(?,?,?)",actor,post,guard.access.now());
        else if(!enabled)db.update("delete from "+table+" where user_id=? and post_id=?",actor,post);return activity(post,actor);
    }
    @Transactional(readOnly=true) public ContentPage<CommunityService.Post> personal(Long actor,String scope,String search,String topic,String type,int page){
        guard.access.actor(actor);ContentAccess.page(page);CommunityService.choice(scope,Set.of("MINE","SAVED"),"Bộ lọc cá nhân không hợp lệ.");
        return posts.filtered(search,topic,type,page,actor,scope);
    }
    public CommunityService.Post answer(Long actor,long postId,AnswerWrite input){
        if(input==null)throw MarketplaceException.invalid("Cần lựa chọn câu trả lời.");guard.writer(actor);posts.lockPost(postId);var p=posts.rawPost(postId);posts.author(actor,p.authorId());posts.writable(p);MarketplaceAccess.revision(p.revision(),ContentAccess.revision(input.revision()));
        if(!Set.of("QUESTION","PLANT_HELP").contains(p.postType()))throw MarketplaceException.invalid("Chỉ bài hỏi đáp hoặc cứu cây được chọn giải pháp.");
        CommunityService.Comment c=null;if(input.commentId()!=null){c=posts.commentRow(input.commentId());if(c.postId()!=postId)throw MarketplaceException.invalid("Bình luận không thuộc bài viết.");}
        if(Objects.equals(p.acceptedCommentId(),input.commentId()))return posts.post(postId);
        db.update("update community_posts set accepted_comment_id=?,help_status=?,revision=revision+1,updated_at=? where id=?",input.commentId(),p.helpStatus().equals("RESOLVED")?"RESOLVED":c==null?"OPEN":"HELPFUL",guard.access.now(),postId);
        if(c!=null)notifications.enqueue(postId,actor,c.authorId(),"COMMUNITY_ACCEPTED","ACCEPT:"+c.id()+":"+(p.revision()+1));return posts.post(postId);
    }
    public CommunityService.Post status(Long actor,long postId,StatusWrite input){
        if(input==null)throw MarketplaceException.invalid("Cần trạng thái.");guard.writer(actor);posts.lockPost(postId);var p=posts.rawPost(postId);posts.author(actor,p.authorId());posts.writable(p);MarketplaceAccess.revision(p.revision(),ContentAccess.revision(input.revision()));
        if(!Set.of("QUESTION","PLANT_HELP").contains(p.postType()))throw MarketplaceException.invalid("Bài này không có trạng thái cần giúp.");CommunityService.choice(input.status(),Set.of("OPEN","RESOLVED"),"Tác giả chỉ xác nhận đã giải quyết hoặc mở lại.");
        String status=input.status().equals("OPEN")&&p.acceptedCommentId()!=null?"HELPFUL":input.status();if(!status.equals(p.helpStatus()))db.update("update community_posts set help_status=?,revision=revision+1,updated_at=? where id=?",status,guard.access.now(),postId);return posts.post(postId);
    }
    @Transactional(readOnly=true) public ContentPage<ProductLink> products(Long actor,int page){
        guard.access.seller(actor);ContentAccess.page(page);var stores=em.createQuery("select s from Store s join fetch s.owner where s.owner.id=:id",Store.class).setParameter("id",actor).getResultList();
        if(stores.isEmpty()||!eligibility.eligible(stores.getFirst()))return ContentPage.of(List.of(),page,0);
        long store=stores.getFirst().getId();String where=" from products p join categories c on c.id=p.category_id join stores s on s.id=p.store_id where p.store_id=? and p.status='ACTIVE' and c.is_active=true";
        long total=db.queryForObject("select count(*)"+where,Long.class,store);var list=db.query("select p.id,p.name,p.price,p.image_url,s.id store_id,s.name store_name"+where+" order by p.id limit 10 offset ?",(r,n)->new ProductLink(r.getLong("id"),r.getString("name"),r.getBigDecimal("price"),r.getString("image_url"),r.getLong("store_id"),r.getString("store_name")),store,page*10);return ContentPage.of(list,page,total);
    }
    public Activity links(Long actor,long postId,LinksWrite input){
        if(input==null||input.productIds()==null||input.productIds().size()>3||input.productIds().stream().anyMatch(id->id==null||id<1)||new HashSet<>(input.productIds()).size()!=input.productIds().size())throw MarketplaceException.invalid("Chọn tối đa 3 sản phẩm khác nhau.");
        guard.writer(actor);posts.lockPost(postId);var p=posts.rawPost(postId);posts.author(actor,p.authorId());posts.writable(p);MarketplaceAccess.revision(p.revision(),ContentAccess.revision(input.revision()));
        if(!input.productIds().isEmpty())guard.access.seller(actor);
        for(long id:input.productIds()){var product=em.find(Product.class,id);if(product==null||!product.getStore().getOwner().getId().equals(actor))throw new MarketplaceException(403,"FORBIDDEN","Chỉ liên kết sản phẩm thuộc gian hàng của bạn.");if(product.getStatus()!=ProductStatus.ACTIVE||!Boolean.TRUE.equals(product.getCategory().getIsActive())||!eligibility.eligible(product.getStore()))throw new MarketplaceException(409,"PRODUCT_UNAVAILABLE","Sản phẩm hoặc gian hàng chưa đủ điều kiện công khai.");}
        db.update("delete from community_product_links where post_id=?",postId);for(long id:input.productIds())db.update("insert into community_product_links(post_id,product_id) values(?,?)",postId,id);
        db.update("update community_posts set revision=revision+1,updated_at=? where id=?",guard.access.now(),postId);return activity(postId,actor);
    }
    private List<ProductLink> linkedProducts(CommunityService.Post post){
        // JDBC link rows remain separate from catalog entities; bounded at three IDs.
        var ids=db.queryForList("select product_id from community_product_links where post_id=? order by product_id",Long.class,post.id());if(ids.isEmpty())return List.of();
        var products=em.createQuery("select p from Product p join fetch p.store s join fetch s.owner join fetch p.category where p.id in :ids order by p.id",Product.class).setParameter("ids",ids).getResultList();var eligible=new HashMap<Long,Boolean>();
        return products.stream().filter(p->p.getStore().getOwner().getId().equals(post.authorId())&&p.getStatus()==ProductStatus.ACTIVE&&Boolean.TRUE.equals(p.getCategory().getIsActive())&&eligible.computeIfAbsent(p.getStore().getId(),id->eligibility.eligible(p.getStore()))).map(p->new ProductLink(p.getId(),p.getName(),p.getPrice(),p.getImageUrl(),p.getStore().getId(),p.getStore().getName())).toList();
    }
    @Transactional(readOnly=true) public ContentPage<NotificationView> notifications(Long actor,int page){
        guard.access.actor(actor);ContentAccess.page(page);String key="COMMUNITY:%:USER:"+actor;
        long total=db.queryForObject("select count(*) from order_notification_outbox where dedup_key like ? and event_type in ('COMMUNITY_COMMENTED','COMMUNITY_ACCEPTED','COMMUNITY_MODERATED')",Long.class,key);
        var rows=db.query("select id,event_type,body,created_at,status from order_notification_outbox where dedup_key like ? and event_type in ('COMMUNITY_COMMENTED','COMMUNITY_ACCEPTED','COMMUNITY_MODERATED') order by created_at desc,id desc limit 10 offset ?",(r,n)->new NotificationView(r.getString("id"),r.getString("event_type"),r.getString("body"),r.getObject("created_at",java.time.LocalDateTime.class),r.getString("status")),key,page*10);return ContentPage.of(rows,page,total);
    }
}
