package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class CommunityModeration {
    private final JdbcTemplate db;private final ContentAccess guard;private final CommunityService posts;private final CommunityNotifications notifications;private final EntityManager em;private final ObjectMapper json;
    public CommunityModeration(JdbcTemplate db,ContentAccess guard,CommunityService posts,CommunityNotifications notifications,EntityManager em,ObjectMapper json){this.db=db;this.guard=guard;this.posts=posts;this.notifications=notifications;this.em=em;this.json=json;}
    public record Target(String kind,long id,long postId,String title,String body,long authorId,String authorName,String authorRole,String status,String parentStatus,boolean locked,boolean pinned,long revision,LocalDateTime createdAt,LocalDateTime updatedAt) {}
    public record Decision(String action,String reason,Long revision) {}
    public record AuditNote(String previousState,String newState,String reason) {}
    public record History(long id,String actorName,String actionLabel,AuditNote detail,LocalDateTime createdAt) {}
    public record ReportHistory(long id,String actorName,String actionLabel,String reason,LocalDateTime createdAt) {}
    private AuditNote presentNote(String stored) {
        try {
            var note=json.readValue(stored,AuditNote.class);
            return new AuditNote(AuditPresentation.state(note.previousState()),AuditPresentation.state(note.newState()),note.reason());
        } catch (Exception ignored) {
            // Legacy/unrecognized audit serialization remains available in the database, never as UI text.
            return new AuditNote("Chưa có thông tin trạng thái","Chưa có thông tin trạng thái",null);
        }
    }
    private String kind(String kind){return CommunityService.choice(kind,Set.of("POST","COMMENT"),"Loại nội dung không hợp lệ.");}
    private String select(String kind){return kind.equals("POST")?"select p.id,p.id post_id,p.title,p.body,p.author_id,u.name author_name,u.role author_role,p.status,p.status parent_status,p.discussion_locked,p.pinned,p.revision,p.created_at,p.updated_at from community_posts p join users u on u.id=p.author_id":"select c.id,c.post_id,p.title,c.body,c.author_id,u.name author_name,u.role author_role,c.status,p.status parent_status,p.discussion_locked,p.pinned,c.revision,c.created_at,c.updated_at from community_comments c join community_posts p on p.id=c.post_id join users u on u.id=c.author_id";}
    private RowMapper<Target> mapper(String kind){return (r,n)->new Target(kind,r.getLong("id"),r.getLong("post_id"),r.getString("title"),r.getString("body"),r.getLong("author_id"),r.getString("author_name"),r.getString("author_role"),r.getString("status"),r.getString("parent_status"),r.getBoolean("discussion_locked"),r.getBoolean("pinned"),r.getLong("revision"),r.getObject("created_at",LocalDateTime.class),r.getObject("updated_at",LocalDateTime.class));}
    @Transactional(readOnly=true) public ContentPage<Target> list(Long actor,String kind,String status,String search,int page){
        guard.access.admin(actor);kind(kind);CommunityService.choice(status,Set.of("PUBLIC","HIDDEN","REMOVED"),"Trạng thái không hợp lệ.");ContentAccess.page(page);search=CommunityService.optional(search,160);String alias=kind.equals("POST")?"p":"c",where=" where "+alias+".status=?";var args=new ArrayList<Object>();args.add(status);
        if(!search.isBlank()){where+=" and (lower(p.title) like ? escape '!' or lower("+alias+".body) like ? escape '!')";String pattern="%"+search.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(pattern);args.add(pattern);}
        String from=select(kind).substring(select(kind).indexOf(" from "));long total=db.queryForObject("select count(*)"+from+where,Long.class,args.toArray());args.add(page*10);
        return ContentPage.of(db.query(select(kind)+where+" order by "+alias+".created_at desc,"+alias+".id desc limit 10 offset ?",mapper(kind),args.toArray()),page,total);
    }
    @Transactional(readOnly=true) public Target detail(Long actor,String kind,long id){guard.access.admin(actor);kind(kind);String alias=kind.equals("POST")?"p":"c";return db.query(select(kind)+" where "+alias+".id=?",mapper(kind),id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    private String state(Target t){return t.status()+"; locked="+t.locked()+"; pinned="+t.pinned()+"; revision="+t.revision();}
    public Target decide(Long actor,String kind,long id,Decision input){
        guard.writer(actor);guard.access.admin(actor);kind(kind);if(input==null)throw MarketplaceException.invalid("Cần quyết định kiểm duyệt.");String reason=ContentAccess.text(input.reason(),300);CommunityService.choice(input.action(),Set.of("HIDE","REMOVE","RESTORE","PIN","UNPIN","LOCK","UNLOCK"),"Hành động kiểm duyệt không hợp lệ.");
        Target initial=detail(actor,kind,id);db.queryForList("select id from community_posts where id=? for update",initial.postId());if(kind.equals("COMMENT"))db.queryForList("select id from community_comments where id=? for update",id);
        Target before=detail(actor,kind,id);MarketplaceAccess.revision(before.revision(),ContentAccess.revision(input.revision()));String status=before.status();boolean locked=before.locked(),pinned=before.pinned();
        switch(input.action()){
            case "HIDE" -> {if(!status.equals("PUBLIC"))throw conflict();status="HIDDEN";}
            case "REMOVE" -> {if(status.equals("REMOVED"))throw conflict();status="REMOVED";}
            case "RESTORE" -> {if(!status.equals("HIDDEN")||kind.equals("COMMENT")&&!before.parentStatus().equals("PUBLIC"))throw conflict();status="PUBLIC";}
            default -> {
                if(!kind.equals("POST")||!status.equals("PUBLIC"))throw conflict();
                if(input.action().equals("PIN")||input.action().equals("UNPIN")){var post=posts.rawPost(id);if(input.action().equals("PIN")&&!Set.of("GUIDE","ANNOUNCEMENT").contains(post.postType()))throw MarketplaceException.invalid("Chỉ ghim hướng dẫn hoặc thông báo.");pinned=input.action().equals("PIN");}
                else locked=input.action().equals("LOCK");
            }
        }
        if(status.equals(before.status())&&locked==before.locked()&&pinned==before.pinned())return before;
        if(kind.equals("POST"))db.update("update community_posts set status=?,discussion_locked=?,pinned=?,revision=revision+1,updated_at=? where id=?",status,locked,pinned,guard.access.now(),id);
        else {db.update("update community_comments set status=?,revision=revision+1,updated_at=? where id=?",status,guard.access.now(),id);if(!status.equals("PUBLIC"))posts.clearAccepted(before.postId(),id);}
        Target after=detail(actor,kind,id);MarketplaceAudit.record(em,guard.access,actor,"COMMUNITY_"+input.action(),"COMMUNITY_"+kind,id,json.writeValueAsString(new AuditNote(state(before),state(after),reason)));
        if(!status.equals("PUBLIC"))notifications.enqueue(before.postId(),actor,before.authorId(),"COMMUNITY_MODERATED",kind+":"+id+":"+(before.revision()+1));return after;
    }
    private MarketplaceException conflict(){return new MarketplaceException(409,"MODERATION_STATE_INVALID","Thao tác không phù hợp trạng thái hiện tại. Tải lại nội dung trước khi quyết định.");}
    @Transactional(readOnly=true) public ContentPage<History> history(Long actor,String kind,long id,int page){
        detail(actor,kind,id);ContentAccess.page(page);String target="COMMUNITY_"+kind;long total=db.queryForObject("select count(*) from marketplace_audit where target_type=? and target_id=?",Long.class,target,id);
        var list=db.query("select a.*,u.name actor_name from marketplace_audit a join users u on u.id=a.actor_id where a.target_type=? and a.target_id=? order by a.created_at desc,a.id desc limit 10 offset ?",(r,n)->new History(r.getLong("id"),r.getString("actor_name"),AuditPresentation.action(r.getString("action")),presentNote(r.getString("reason")),r.getObject("created_at",LocalDateTime.class)),target,id,page*10);return ContentPage.of(list,page,total);
    }
    @Transactional(readOnly=true) public ContentPage<ReportHistory> reportHistory(Long actor,long id,int page){
        guard.access.admin(actor);ContentAccess.page(page);if(db.queryForObject("select count(*) from marketplace_reports where id=? and target_type in ('COMMUNITY_POST','COMMUNITY_COMMENT')",Long.class,id)==0)throw MarketplaceException.missing();
        long total=db.queryForObject("select count(*) from marketplace_audit where target_type='REPORT' and target_id=?",Long.class,id);
        var rows=db.query("select a.*,u.name actor_name from marketplace_audit a join users u on u.id=a.actor_id where a.target_type='REPORT' and a.target_id=? order by a.created_at,a.id limit 10 offset ?",(r,n)->new ReportHistory(r.getLong("id"),r.getString("actor_name"),AuditPresentation.action(r.getString("action")),AuditPresentation.reason(r.getString("action"),r.getString("reason")),r.getObject("created_at",LocalDateTime.class)),id,page*10);return ContentPage.of(rows,page,total);
    }
}
