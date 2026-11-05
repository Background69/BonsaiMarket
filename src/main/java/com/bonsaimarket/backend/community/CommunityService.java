package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

/** Plain-text content. Author edits and administrative moderation are separate contracts. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CommunityService {
    final JdbcTemplate db;
    final ContentAccess guard;
    private final CommunityNotifications notifications;
    public CommunityService(JdbcTemplate db,ContentAccess guard,CommunityNotifications notifications){this.db=db;this.guard=guard;this.notifications=notifications;}
    public record Care(String plantName,String symptoms,String duration,String lighting,String watering,String substrate,String attemptedCare) {}
    public record Image(String id,String url) {}
    public record Post(long id,long authorId,String authorName,String authorRole,String title,String body,String topic,String postType,
                       Care care,String helpStatus,Long acceptedCommentId,boolean locked,boolean pinned,LocalDateTime createdAt,
                       LocalDateTime updatedAt,long revision,List<Image> images,long likeCount,long commentCount) {}
    public record Comment(long id,long postId,long authorId,String authorName,String authorRole,String body,LocalDateTime createdAt,long revision) {}
    public record PostWrite(String title,String body,String topic,Long revision,String postType,Care care,String requestKey) {}
    public record CommentWrite(String body,Long revision,String requestKey) {}
    static final String POST="select p.*,u.name author_name,u.role author_role,(select count(*) from community_likes l where l.post_id=p.id) like_count,(select count(*) from community_comments c where c.post_id=p.id and c.status='PUBLIC') comment_count from community_posts p join users u on u.id=p.author_id";
    private static final String COMMENT="select c.*,u.name author_name,u.role author_role from community_comments c join users u on u.id=c.author_id";
    final RowMapper<Post> postMapper=(r,n)->new Post(r.getLong("id"),r.getLong("author_id"),r.getString("author_name"),r.getString("author_role"),r.getString("title"),r.getString("body"),r.getString("topic"),r.getString("post_type"),
        new Care(r.getString("plant_name"),r.getString("symptoms"),r.getString("symptom_duration"),r.getString("lighting"),r.getString("watering"),r.getString("substrate"),r.getString("attempted_care")),
        r.getString("help_status"),(Long)r.getObject("accepted_comment_id"),r.getBoolean("discussion_locked"),r.getBoolean("pinned"),r.getObject("created_at",LocalDateTime.class),r.getObject("updated_at",LocalDateTime.class),r.getLong("revision"),List.of(),r.getLong("like_count"),r.getLong("comment_count"));
    private final RowMapper<Comment> commentMapper=(r,n)->new Comment(r.getLong("id"),r.getLong("post_id"),r.getLong("author_id"),r.getString("author_name"),r.getString("author_role"),r.getString("body"),r.getObject("created_at",LocalDateTime.class),r.getLong("revision"));
    private static String topic(String value){return choice(value,Set.of("BONSAI","SUCCULENTS","CARE","OTHER"),"Chủ đề không hợp lệ.");}
    static String choice(String value,Set<String> allowed,String message){if(!allowed.contains(value==null?"":value))throw MarketplaceException.invalid(message);return value;}
    static String optional(String value,int max){if(value==null||value.isBlank())return "";return ContentAccess.text(value,max);}
    static String requestKey(String value){if(value==null)return null;try{if(!UUID.fromString(value).toString().equals(value))throw new IllegalArgumentException();return value;}catch(IllegalArgumentException e){throw MarketplaceException.invalid("Mã thao tác không hợp lệ.");}}
    static String hash(Object value){
        String text=value.toString();if(value instanceof Draft d){var framed=new StringBuilder();for(String part:List.of(d.title,d.body,d.topic,d.type,d.care.plantName,d.care.symptoms,d.care.duration,d.care.lighting,d.care.watering,d.care.substrate,d.care.attemptedCare))framed.append(part.length()).append(':').append(part);text=framed.toString();}
        try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    void author(Long actor,long author){if(!Objects.equals(actor,author))throw new MarketplaceException(403,"FORBIDDEN","Chỉ tác giả được chỉnh sửa nội dung. Quản trị dùng thao tác kiểm duyệt riêng.");}
    @Transactional(readOnly=true)
    public ContentPage<Post> list(String search,String topic,String type,int page){
        return filtered(search,topic,type,page,null,null);
    }
    ContentPage<Post> filtered(String search,String topic,String type,int page,Long actor,String scope){
        ContentAccess.page(page);search=java.text.Normalizer.normalize(search==null?"":search,java.text.Normalizer.Form.NFC).replaceAll("(?U)\\s+"," ").strip();
        if(search.length()>160)throw MarketplaceException.invalid("Từ khóa tối đa 160 ký tự.");
        String where=" where p.status='PUBLIC'";var args=new ArrayList<Object>();
        if(actor!=null){guard.access.actor(actor);choice(scope,Set.of("MINE","SAVED"),"Bộ lọc cá nhân không hợp lệ.");where+=scope.equals("MINE")?" and p.author_id=?":" and exists(select 1 from community_bookmarks b where b.post_id=p.id and b.user_id=?)";args.add(actor);}
        if(!topic.isBlank()){topic(topic);where+=" and p.topic=?";args.add(topic);}
        if(!type.isBlank()){type(type);where+=" and p.post_type=?";args.add(type);}
        if(!search.isBlank()){where+=" and (lower(p.title) like ? escape '!' or lower(p.body) like ? escape '!')";String pattern="%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(pattern);args.add(pattern);}
        long count=db.queryForObject("select count(*) from community_posts p"+where,Long.class,args.toArray());args.add(page*10);
        return ContentPage.of(withImages(db.query(POST+where+" order by p.pinned desc,p.created_at desc,p.id desc limit 10 offset ?",postMapper,args.toArray())),page,count);
    }
    static String type(String value){return choice(value,Set.of("QUESTION","SHOWCASE","GUIDE","PLANT_HELP","ANNOUNCEMENT"),"Loại bài viết không hợp lệ.");}
    @Transactional(readOnly=true) public Post post(long id){return withImages(List.of(rawPost(id))).getFirst();}
    @Transactional(readOnly=true) public Object highlights(){
        return Map.of("pinned",queryPosts(" where p.status='PUBLIC' and p.pinned=true order by p.created_at desc,p.id desc limit 3"),"topics",db.queryForList("select topic,count(*) postCount from community_posts where status='PUBLIC' group by topic order by count(*) desc,topic"));
    }
    Post rawPost(long id){return db.query(POST+" where p.id=? and p.status='PUBLIC'",postMapper,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    List<Post> queryPosts(String suffix,Object... args){return withImages(db.query(POST+suffix,postMapper,args));}
    List<Post> withImages(List<Post> posts){
        if(posts.isEmpty())return posts;var images=new HashMap<Long,List<Image>>();String marks=String.join(",",Collections.nCopies(posts.size(),"?"));
        db.query("select id,post_id from community_images where post_id in ("+marks+") order by created_at,id",r->{long id=r.getLong("post_id");String key=r.getString("id");images.computeIfAbsent(id,k->new ArrayList<>()).add(new Image(key,"/api/community/posts/"+id+"/images/"+key));},posts.stream().map(Post::id).toArray());
        return posts.stream().map(p->new Post(p.id,p.authorId,p.authorName,p.authorRole,p.title,p.body,p.topic,p.postType,p.care,p.helpStatus,p.acceptedCommentId,p.locked,p.pinned,p.createdAt,p.updatedAt,p.revision,images.getOrDefault(p.id,List.of()),p.likeCount,p.commentCount)).toList();
    }
    @Transactional(readOnly=true) public ContentPage<Comment> comments(long postId,int page){
        rawPost(postId);ContentAccess.page(page);long count=db.queryForObject("select count(*) from community_comments where post_id=? and status='PUBLIC'",Long.class,postId);
        return ContentPage.of(db.query(COMMENT+" where c.post_id=? and c.status='PUBLIC' order by c.created_at,c.id limit 10 offset ?",commentMapper,postId,page*10),page,count);
    }
    private record Draft(String title,String body,String topic,String type,Care care) {}
    private Draft validate(Long actor,PostWrite input,String oldType){
        if(input==null)throw MarketplaceException.invalid("Cần nội dung bài viết.");
        String type=type(input.postType()==null?(oldType==null?"GUIDE":oldType):input.postType());if(type.equals("ANNOUNCEMENT"))guard.access.admin(actor);
        Care c=input.care();c=c==null?new Care("","","","","","",""):c;
        c=new Care(optional(c.plantName,180),optional(c.symptoms,1000),optional(c.duration,180),optional(c.lighting,500),optional(c.watering,500),optional(c.substrate,500),optional(c.attemptedCare,1000));
        if(!type.equals("PLANT_HELP"))c=new Care("","","","","","","");
        return new Draft(ContentAccess.text(input.title(),180),ContentAccess.text(input.body(),12000),topic(input.topic()),type,c);
    }
    public Post create(Long actor,PostWrite input){
        guard.writer(actor);Draft d=validate(actor,input,null);String key=requestKey(input.requestKey()),hash=hash(d);
        if(key!=null){var prior=db.queryForList("select id,request_hash,status from community_posts where author_id=? and request_key=?",actor,key);if(!prior.isEmpty()){var p=prior.getFirst();if(!hash.equals(p.get("request_hash"))||!"PUBLIC".equals(p.get("status")))throw new MarketplaceException(409,"REQUEST_CONFLICT","Mã thao tác đã được dùng cho nội dung khác.");return post(((Number)p.get("id")).longValue());}}
        guard.throttle("community_posts",actor,5,d.body,null,null);
        db.update("insert into community_posts(author_id,title,body,topic,post_type,plant_name,symptoms,symptom_duration,lighting,watering,substrate,attempted_care,request_key,request_hash,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",actor,d.title,d.body,d.topic,d.type,d.care.plantName,d.care.symptoms,d.care.duration,d.care.lighting,d.care.watering,d.care.substrate,d.care.attemptedCare,key,hash,guard.access.now(),guard.access.now());
        return post(db.queryForObject("select max(id) from community_posts where author_id=?",Long.class,actor));
    }
    public Post edit(long id,Long actor,PostWrite input){
        guard.writer(actor);lockPost(id);var old=rawPost(id);author(actor,old.authorId());writable(old);MarketplaceAccess.revision(old.revision(),ContentAccess.revision(input.revision()));Draft d=validate(actor,input,old.postType);
        if(old.acceptedCommentId!=null&&!d.type.equals(old.postType))throw new MarketplaceException(409,"ANSWER_PRESENT","Bỏ chọn giải pháp trước khi đổi loại bài.");
        if(old.pinned&&!Set.of("GUIDE","ANNOUNCEMENT").contains(d.type))throw new MarketplaceException(409,"POST_PINNED","Cần quản trị bỏ ghim trước khi chuyển sang loại bài này.");
        db.update("update community_posts set title=?,body=?,topic=?,post_type=?,plant_name=?,symptoms=?,symptom_duration=?,lighting=?,watering=?,substrate=?,attempted_care=?,updated_at=?,revision=revision+1 where id=?",d.title,d.body,d.topic,d.type,d.care.plantName,d.care.symptoms,d.care.duration,d.care.lighting,d.care.watering,d.care.substrate,d.care.attemptedCare,guard.access.now(),id);return post(id);
    }
    public void removePost(long id,Long actor,long revision){
        guard.writer(actor);lockPost(id);var old=rawPost(id);author(actor,old.authorId());writable(old);MarketplaceAccess.revision(old.revision(),revision);
        db.update("update community_posts set status='REMOVED',revision=revision+1,updated_at=? where id=?",guard.access.now(),id);
    }
    void lockPost(long id){if(db.queryForList("select id from community_posts where id=? and status='PUBLIC' for update",id).isEmpty())throw MarketplaceException.missing();}
    void writable(Post post){if(post.locked)throw new MarketplaceException(409,"DISCUSSION_LOCKED","Thảo luận đã đóng; nội dung không thể thay đổi.");}
    public Comment comment(long postId,Long actor,CommentWrite input){
        if(input==null)throw MarketplaceException.invalid("Cần nội dung bình luận.");
        guard.writer(actor);lockPost(postId);writable(rawPost(postId));String body=ContentAccess.text(input.body(),3000),key=requestKey(input.requestKey()),hash=hash(postId+":"+body);
        if(key!=null){var prior=db.queryForList("select id,request_hash,status from community_comments where author_id=? and request_key=?",actor,key);if(!prior.isEmpty()){var p=prior.getFirst();if(!hash.equals(p.get("request_hash"))||!"PUBLIC".equals(p.get("status")))throw new MarketplaceException(409,"REQUEST_CONFLICT","Mã thao tác đã được dùng.");return commentRow(((Number)p.get("id")).longValue());}}
        guard.throttle("community_comments",actor,30,body,"post_id",postId);
        db.update("insert into community_comments(post_id,author_id,body,request_key,request_hash,created_at,updated_at) values(?,?,?,?,?,?,?)",postId,actor,body,key,hash,guard.access.now(),guard.access.now());
        var comment=commentRow(db.queryForObject("select max(id) from community_comments where author_id=?",Long.class,actor));
        notifications.enqueue(postId,actor,rawPost(postId).authorId(),"COMMUNITY_COMMENTED","COMMENT:"+comment.id());return comment;
    }
    Comment commentRow(long id){return db.query(COMMENT+" where c.id=? and c.status='PUBLIC'",commentMapper,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    public Comment editComment(long id,Long actor,CommentWrite input){
        if(input==null)throw MarketplaceException.invalid("Cần nội dung bình luận.");
        guard.writer(actor);var old=commentRow(id);lockPost(old.postId());writable(rawPost(old.postId()));old=commentRow(id);author(actor,old.authorId());MarketplaceAccess.revision(old.revision(),ContentAccess.revision(input.revision()));
        db.update("update community_comments set body=?,revision=revision+1,updated_at=? where id=?",ContentAccess.text(input.body(),3000),guard.access.now(),id);clearAccepted(old.postId(),id);return commentRow(id);
    }
    public void removeComment(long id,Long actor,long revision){
        guard.writer(actor);var old=commentRow(id);lockPost(old.postId());writable(rawPost(old.postId()));old=commentRow(id);author(actor,old.authorId());MarketplaceAccess.revision(old.revision(),revision);clearAccepted(old.postId(),id);
        db.update("update community_comments set status='REMOVED',revision=revision+1,updated_at=? where id=?",guard.access.now(),id);
    }
    void clearAccepted(long post,long comment){db.update("update community_posts set accepted_comment_id=null,help_status=case when help_status='HELPFUL' then 'OPEN' else help_status end,revision=revision+1,updated_at=? where id=? and accepted_comment_id=?",guard.access.now(),post,comment);}
}
