package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** Thảo luận là văn bản thuần; phản hồi công khai không chứa thông tin tài khoản riêng tư. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CommunityService {
    private final JdbcTemplate db;private final ContentAccess guard;
    public CommunityService(JdbcTemplate db,ContentAccess guard){this.db=db;this.guard=guard;}
    public record Post(long id,long authorId,String authorName,String title,String body,String topic,LocalDateTime createdAt,LocalDateTime updatedAt,long revision) {}
    public record Comment(long id,long postId,long authorId,String authorName,String body,LocalDateTime createdAt,long revision) {}
    public record PostWrite(String title,String body,String topic,Long revision) {}
    public record CommentWrite(String body,Long revision) {}
    private static final String POST="select p.id,p.author_id,u.name author_name,p.title,p.body,p.topic,p.created_at,p.updated_at,p.revision from community_posts p join users u on u.id=p.author_id";
    private static final String COMMENT="select c.id,c.post_id,c.author_id,u.name author_name,c.body,c.created_at,c.revision from community_comments c join users u on u.id=c.author_id";
    private final RowMapper<Post> postMapper=(r,n)->new Post(r.getLong("id"),r.getLong("author_id"),r.getString("author_name"),r.getString("title"),r.getString("body"),r.getString("topic"),r.getObject("created_at",LocalDateTime.class),r.getObject("updated_at",LocalDateTime.class),r.getLong("revision"));
    private final RowMapper<Comment> commentMapper=(r,n)->new Comment(r.getLong("id"),r.getLong("post_id"),r.getLong("author_id"),r.getString("author_name"),r.getString("body"),r.getObject("created_at",LocalDateTime.class),r.getLong("revision"));
    private static String topic(String topic){if(!Set.of("BONSAI","SUCCULENTS","CARE","OTHER").contains(topic==null?"":topic))throw MarketplaceException.invalid("Chủ đề không hợp lệ.");return topic;}
    @Transactional(readOnly=true)
    public ContentPage<Post> list(String search,String topic,int page){
        ContentAccess.page(page);search=java.text.Normalizer.normalize(search,java.text.Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip();
        if(search.length()>160)throw MarketplaceException.invalid("Từ khóa tối đa 160 ký tự.");
        String where=" where p.status='PUBLIC'";var args=new ArrayList<Object>();
        if(!topic.isBlank()){topic(topic);where+=" and p.topic=?";args.add(topic);}
        if(!search.isBlank()){where+=" and (lower(p.title) like ? escape '!' or lower(p.body) like ? escape '!')";String pattern="%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(pattern);args.add(pattern);}
        long count=db.queryForObject("select count(*) from community_posts p"+where,Long.class,args.toArray());
        args.add(page*10);
        return ContentPage.of(db.query(POST+where+" order by p.created_at desc,p.id desc limit 10 offset ?",postMapper,args.toArray()),page,count);
    }
    @Transactional(readOnly=true) public Post post(long id){return db.query(POST+" where p.id=? and p.status='PUBLIC'",postMapper,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    @Transactional(readOnly=true) public ContentPage<Comment> comments(long postId,int page){
        post(postId);ContentAccess.page(page);
        long count=db.queryForObject("select count(*) from community_comments where post_id=? and status='PUBLIC'",Long.class,postId);
        return ContentPage.of(db.query(COMMENT+" where c.post_id=? and c.status='PUBLIC' order by c.created_at,c.id limit 10 offset ?",commentMapper,postId,page*10),page,count);
    }
    public Post create(Long actor,PostWrite input){
        guard.writer(actor);String title=ContentAccess.text(input.title(),180),body=ContentAccess.text(input.body(),12000),topic=topic(input.topic());
        guard.throttle("community_posts",actor,5,body,null,null);
        db.update("insert into community_posts(author_id,title,body,topic,created_at,updated_at) values(?,?,?,?,?,?)",actor,title,body,topic,guard.access.now(),guard.access.now());
        return post(db.queryForObject("select max(id) from community_posts where author_id=?",Long.class,actor));
    }
    public Post edit(long id,Long actor,PostWrite input){
        var user=guard.writer(actor);lockPost(id);var old=post(id);guard.owner(user,old.authorId());MarketplaceAccess.revision(old.revision(),ContentAccess.revision(input.revision()));
        db.update("update community_posts set title=?,body=?,topic=?,updated_at=?,revision=revision+1 where id=?",ContentAccess.text(input.title(),180),ContentAccess.text(input.body(),12000),topic(input.topic()),guard.access.now(),id);
        return post(id);
    }
    public void removePost(long id,Long actor,long revision){
        var user=guard.writer(actor);lockPost(id);var old=post(id);guard.owner(user,old.authorId());MarketplaceAccess.revision(old.revision(),revision);
        db.update("update community_posts set status='REMOVED',revision=revision+1,updated_at=? where id=?",guard.access.now(),id);
    }
    private void lockPost(long id){if(db.queryForList("select id from community_posts where id=? and status='PUBLIC' for update",id).isEmpty())throw MarketplaceException.missing();}
    public Comment comment(long postId,Long actor,CommentWrite input){
        guard.writer(actor);lockPost(postId);String body=ContentAccess.text(input.body(),3000);
        guard.throttle("community_comments",actor,30,body,"post_id",postId);
        db.update("insert into community_comments(post_id,author_id,body,created_at,updated_at) values(?,?,?,?,?)",postId,actor,body,guard.access.now(),guard.access.now());
        return comment(db.queryForObject("select max(id) from community_comments where author_id=?",Long.class,actor));
    }
    private Comment comment(long id){return db.query(COMMENT+" where c.id=? and c.status='PUBLIC'",commentMapper,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    public Comment editComment(long id,Long actor,CommentWrite input){
        var user=guard.writer(actor);var old=comment(id);lockPost(old.postId());
        if(db.queryForList("select id from community_comments where id=? and status='PUBLIC' for update",id).isEmpty())throw MarketplaceException.missing();
        old=comment(id);guard.owner(user,old.authorId());MarketplaceAccess.revision(old.revision(),ContentAccess.revision(input.revision()));
        db.update("update community_comments set body=?,revision=revision+1,updated_at=? where id=?",ContentAccess.text(input.body(),3000),guard.access.now(),id);return comment(id);
    }
    public void removeComment(long id,Long actor,long revision){
        var user=guard.writer(actor);var old=comment(id);lockPost(old.postId());db.queryForList("select id from community_comments where id=? for update",id);
        old=comment(id);guard.owner(user,old.authorId());MarketplaceAccess.revision(old.revision(),revision);
        db.update("update community_comments set status='REMOVED',revision=revision+1,updated_at=? where id=?",guard.access.now(),id);
    }
}
