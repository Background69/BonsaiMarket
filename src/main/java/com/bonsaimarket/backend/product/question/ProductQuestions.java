package com.bonsaimarket.backend.product.question;

import com.bonsaimarket.backend.community.ContentAccess;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.store.StoreStatus;
import com.bonsaimarket.backend.user.UserRole;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class ProductQuestions {
    private final JdbcTemplate db;private final ContentAccess guard;private final ProductRepository products;
    public ProductQuestions(JdbcTemplate db,ContentAccess guard,ProductRepository products){this.db=db;this.guard=guard;this.products=products;}
    public record Question(long id,long productId,long authorId,String authorName,String body,String status,Long answerAuthorId,String answerAuthorName,String answerBody,LocalDateTime answeredAt,LocalDateTime createdAt,long revision,boolean canAnswer) {}
    public record Write(String body,Long revision) {}
    public record Reply(long id,long questionId,long authorId,String authorName,String authorLabel,String body,LocalDateTime createdAt) {}
    private Product product(long id){return products.findByIdAndStatus(id,ProductStatus.ACTIVE).filter(p->p.getStore().getStatus()==StoreStatus.ACTIVE&&Boolean.TRUE.equals(p.getCategory().getIsActive())&&Boolean.TRUE.equals(p.getStore().getOwner().getIsActive())).orElseThrow(MarketplaceException::missing);}
    private RowMapper<Question> mapper(boolean canAnswer){return (r,n)->new Question(r.getLong("id"),r.getLong("product_id"),r.getLong("author_id"),r.getString("author_name"),r.getString("body"),r.getString("status"),(Long)r.getObject("answer_author_id"),r.getString("answer_name"),r.getString("answer_body"),r.getObject("answered_at",LocalDateTime.class),r.getObject("created_at",LocalDateTime.class),r.getLong("revision"),canAnswer);}
    private static final String SELECT="select q.*,u.name author_name,a.name answer_name from product_questions q join users u on u.id=q.author_id left join users a on a.id=q.answer_author_id";
    @Transactional(readOnly=true)
    public ContentPage<Question> list(long productId,int page,Long actor){
        var p=product(productId);ContentAccess.page(page);
        boolean owner=actor!=null&&p.getStore().getOwner().getId().equals(actor)&&java.util.Set.of(UserRole.SELLER,UserRole.ADMIN).contains(p.getStore().getOwner().getRole());
        long count=db.queryForObject("select count(*) from product_questions where product_id=? and status='PUBLIC'",Long.class,productId);
        return ContentPage.of(db.query(SELECT+" where q.product_id=? and q.status='PUBLIC' order by q.created_at desc,q.id desc limit 10 offset ?",mapper(owner),productId,page*10),page,count);
    }
    public Question ask(long productId,Long actor,Write input){
        guard.writer(actor);product(productId);String body=ContentAccess.text(input.body(),2000);
        guard.throttle("product_questions",actor,20,body,"product_id",productId);
        db.update("insert into product_questions(product_id,author_id,body,created_at,updated_at) values(?,?,?,?,?)",productId,actor,body,guard.access.now(),guard.access.now());
        return byId(db.queryForObject("select max(id) from product_questions where author_id=?",Long.class,actor),false);
    }
    private Question byId(long id,boolean owner){return db.query(SELECT+" where q.id=?",mapper(owner),id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    public Question answer(long id,Long actor,Write input){
        var user=guard.writer(actor);
        var locked=db.queryForList("select id from product_questions where id=? and status='PUBLIC' for update",id);
        if(locked.isEmpty())throw MarketplaceException.missing();
        var q=byId(id,false);var p=product(q.productId());
        if(!p.getStore().getOwner().getId().equals(actor)||!java.util.Set.of(UserRole.SELLER,UserRole.ADMIN).contains(user.getRole()))throw new MarketplaceException(403,"FORBIDDEN","Chỉ chủ gian hàng của sản phẩm được trả lời.");
        MarketplaceAccess.revision(q.revision(),ContentAccess.revision(input.revision()));String body=ContentAccess.text(input.body(),4000);
        if(db.queryForObject("select count(*) from product_questions where answer_author_id=? and answered_at>=?",Long.class,actor,guard.access.now().minusHours(1))>=30)throw new MarketplaceException(429,"CONTENT_RATE_LIMIT","Vui lòng thử lại sau.");
        db.update("update product_questions set answer_author_id=?,answer_body=?,answered_at=?,updated_at=?,revision=revision+1 where id=?",actor,body,guard.access.now(),guard.access.now(),id);
        return byId(id,true);
    }
    private Question publicQuestion(long productId,long questionId,boolean lock) {
        product(productId);
        var rows=db.queryForList("select id from product_questions where id=? and product_id=? and status='PUBLIC'"+(lock?" for update":""),questionId,productId);
        if(rows.isEmpty()) throw MarketplaceException.missing();
        return byId(questionId,false);
    }
    @Transactional(readOnly=true)
    public ContentPage<Reply> replies(long productId,long questionId,int page) {
        publicQuestion(productId,questionId,false);ContentAccess.page(page);
        long total=db.queryForObject("select count(*) from product_question_replies where question_id=?",Long.class,questionId);
        var rows=db.query("select r.*,u.name author_name,case when u.id=s.owner_user_id and u.role in ('SELLER','ADMIN') then 'SELLER' when u.role='ADMIN' then 'ADMIN' else 'MEMBER' end author_label from product_question_replies r join users u on u.id=r.author_id join product_questions q on q.id=r.question_id join products p on p.id=q.product_id join stores s on s.id=p.store_id where r.question_id=? order by r.created_at,r.id limit 10 offset ?",
            (r,n)->new Reply(r.getLong("id"),r.getLong("question_id"),r.getLong("author_id"),r.getString("author_name"),r.getString("author_label"),r.getString("body"),r.getObject("created_at",LocalDateTime.class)),questionId,page*10);
        return ContentPage.of(rows,page,total);
    }
    public void reply(long productId,long questionId,Long actor,Write input) {
        guard.writer(actor);publicQuestion(productId,questionId,true);
        String body=ContentAccess.text(input.body(),4000);
        guard.throttle("product_question_replies",actor,30,body,"question_id",questionId);
        db.update("insert into product_question_replies(question_id,author_id,body,created_at) values(?,?,?,?)",questionId,actor,body,guard.access.now());
    }
    public void remove(long id,Long actor,long revision){
        var user=guard.writer(actor);db.queryForList("select id from product_questions where id=? for update",id);var q=byId(id,false);
        guard.owner(user,q.authorId());MarketplaceAccess.revision(q.revision(),revision);
        db.update("update product_questions set status='REMOVED',revision=revision+1,updated_at=? where id=?",guard.access.now(),id);
    }
}
