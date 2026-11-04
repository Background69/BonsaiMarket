package com.bonsaimarket.backend.messaging;

import com.bonsaimarket.backend.community.ContentAccess;
import com.bonsaimarket.backend.community.ContentAccess.ContentPage;
import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.store.*;
import com.bonsaimarket.backend.user.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Durable, participant-scoped human conversations. No public/AI projection. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class PrivateMessaging {
    public record Start(Long storeId,Long supportUserId) {}
    public record Send(String body,String clientKey) {}
    public record Read(Long sequence) {}
    public record Conversation(long id,String kind,Long storeId,String storeName,String peerName,String peerRole,long unreadCount,LocalDateTime updatedAt) {}
    public record Message(long sequence,long senderId,String senderName,String body,LocalDateTime createdAt,boolean mine) {}
    public record Messages(List<Message> content,boolean hasMore,long lastSequence) {}
    private final JdbcTemplate db;
    private final ContentAccess guard;
    public PrivateMessaging(JdbcTemplate db,ContentAccess guard){this.db=db;this.guard=guard;}
    private RowMapper<Conversation> mapper(){return (r,n)->new Conversation(r.getLong("id"),r.getString("kind"),(Long)r.getObject("store_id"),r.getString("store_name"),r.getString("peer_name"),r.getString("peer_role"),r.getLong("unread_count"),r.getObject("updated_at",LocalDateTime.class));}
    private static final String SELECT="select c.*,s.name store_name,u.name peer_name,u.role peer_role,(select count(*) from private_messages m where m.conversation_id=c.id and m.sequence_no>mine.last_read_sequence and m.sender_id<>mine.user_id) unread_count from private_conversations c join private_participants mine on mine.conversation_id=c.id join private_participants peer on peer.conversation_id=c.id and peer.user_id<>mine.user_id join users u on u.id=peer.user_id left join stores s on s.id=c.store_id";
    private long participant(Long actor,long id,boolean lock){
        guard.access.actor(actor);
        if(db.queryForObject("select count(*) from private_participants where conversation_id=? and user_id=?",Long.class,id,actor)==0)throw MarketplaceException.missing();
        return db.queryForObject("select next_sequence from private_conversations where id=?"+(lock?" for update":""),Long.class,id);
    }
    @Transactional(readOnly=true)
    public Conversation detail(Long actor,long id){participant(actor,id,false);return db.query(SELECT+" where c.id=? and mine.user_id=?",mapper(),id,actor).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    @Transactional(readOnly=true)
    public ContentPage<Conversation> list(Long actor,int page){guard.access.actor(actor);ContentAccess.page(page);
        long total=db.queryForObject("select count(*) from private_participants where user_id=?",Long.class,actor);
        return ContentPage.of(db.query(SELECT+" where mine.user_id=? order by c.updated_at desc,c.id desc limit 10 offset ?",mapper(),actor,page*10),page,total);
    }
    @Transactional(readOnly=true)
    public long unread(Long actor){guard.access.actor(actor);return db.queryForObject("select count(*) from private_messages m join private_participants p on p.conversation_id=m.conversation_id and p.user_id=? where m.sequence_no>p.last_read_sequence and m.sender_id<>p.user_id",Long.class,actor);}
    public Conversation start(Long actor,Start input){
        var self=guard.access.actor(actor);
        if((input.storeId()==null)==(input.supportUserId()==null))throw MarketplaceException.invalid("Chọn một gian hàng hoặc một tài khoản cần hỗ trợ.");
        Long peer;String kind,key;
        if(input.storeId()!=null){
            var s=guard.access.actor(actor).getRole();
            if(s==UserRole.ADMIN)throw new MarketplaceException(403,"FORBIDDEN","Admin dùng hội thoại hỗ trợ do chính mình tham gia.");
            var store=guard.access.lock(Store.class,input.storeId());
            if(store.getStatus()!=StoreStatus.ACTIVE||!Boolean.TRUE.equals(store.getOwner().getIsActive())||!Set.of(UserRole.SELLER,UserRole.ADMIN).contains(store.getOwner().getRole()))throw MarketplaceException.missing();
            peer=store.getOwner().getId();kind="STORE";key="STORE:"+store.getId()+":BUYER:"+actor;
        }else{
            guard.access.admin(actor);peer=input.supportUserId();var target=guard.access.actor(peer);
            if(target.getRole()==UserRole.ADMIN)throw MarketplaceException.invalid("Hỗ trợ chỉ dành cho Buyer hoặc Seller.");
            kind="SUPPORT";key="SUPPORT:"+actor+":"+peer;
        }
        if(actor.equals(peer))throw MarketplaceException.invalid("Không tạo hội thoại với chính mình.");
        // Stable lock order serializes duplicate creation even when participants differ as initiators.
        guard.access.lock(User.class,Math.min(actor,peer));guard.access.lock(User.class,Math.max(actor,peer));
        var existing=db.query("select id from private_conversations where conversation_key=?",(r,n)->r.getLong(1),key);
        if(!existing.isEmpty())return detail(actor,existing.getFirst());
        if(db.queryForObject("select count(*) from private_conversations where created_by=? and created_at>=?",Long.class,actor,guard.access.now().minusHours(1))>=20)throw new MarketplaceException(429,"MESSAGE_RATE_LIMIT","Bạn tạo hội thoại quá nhanh. Thử lại sau.");
        db.update("insert into private_conversations(conversation_key,kind,store_id,created_by,created_at,updated_at) values(?,?,?,?,?,?)",key,kind,input.storeId(),actor,guard.access.now(),guard.access.now());
        long id=db.queryForObject("select id from private_conversations where conversation_key=?",Long.class,key);
        db.update("insert into private_participants(conversation_id,user_id) values(?,?),(?,?)",id,actor,id,peer);
        return detail(actor,id);
    }
    @Transactional(readOnly=true)
    public Messages messages(Long actor,long id,Long after,Long before){
        long last=participant(actor,id,false);
        if(after!=null&&after<0||before!=null&&(before<1||after!=null))throw MarketplaceException.invalid("Mốc phân trang tin nhắn không hợp lệ.");
        String where=before!=null?"m.sequence_no<?":after!=null?"m.sequence_no>?":"m.sequence_no>0";
        boolean newest=before!=null||after==null;
        var args=new ArrayList<Object>();args.add(id);if(before!=null)args.add(before);else if(after!=null)args.add(after);
        var rows=db.query("select m.*,u.name sender_name from private_messages m join users u on u.id=m.sender_id where m.conversation_id=? and "+where+" order by m.sequence_no "+(newest?"desc":"asc")+" limit 51",
            (r,n)->new Message(r.getLong("sequence_no"),r.getLong("sender_id"),r.getString("sender_name"),r.getString("body"),r.getObject("created_at",LocalDateTime.class),r.getLong("sender_id")==actor),args.toArray());
        boolean more=rows.size()>50;var content=new ArrayList<>(rows.subList(0,Math.min(50,rows.size())));if(newest)Collections.reverse(content);
        return new Messages(content,more,last);
    }
    public Message send(Long actor,long id,Send input){
        guard.writer(actor);long last=participant(actor,id,true);
        String body=ContentAccess.text(input.body(),4000),key=input.clientKey();
        if(key==null||!key.matches("[A-Za-z0-9_-]{16,64}"))throw MarketplaceException.invalid("Cần mã chống gửi trùng hợp lệ.");
        var retry=db.queryForList("select sequence_no,body from private_messages where conversation_id=? and sender_id=? and client_key=?",id,actor,key);
        if(!retry.isEmpty()){
            if(!retry.getFirst().get("body").equals(body))throw new MarketplaceException(409,"MESSAGE_KEY_REUSED","Mã gửi lại không khớp nội dung ban đầu.");
            return message(actor,id,((Number)retry.getFirst().get("sequence_no")).longValue());
        }
        if(db.queryForObject("select count(*) from private_messages where sender_id=? and created_at>=?",Long.class,actor,guard.access.now().minusMinutes(1))>=30)throw new MarketplaceException(429,"MESSAGE_RATE_LIMIT","Bạn gửi tin quá nhanh. Thử lại sau.");
        long next=last+1;
        db.update("insert into private_messages(conversation_id,sequence_no,sender_id,client_key,body,created_at) values(?,?,?,?,?,?)",id,next,actor,key,body,guard.access.now());
        db.update("update private_conversations set next_sequence=?,updated_at=? where id=?",next,guard.access.now(),id);
        return message(actor,id,next);
    }
    private Message message(Long actor,long id,long sequence){return db.query("select m.*,u.name sender_name from private_messages m join users u on u.id=m.sender_id where m.conversation_id=? and sequence_no=?",(r,n)->new Message(sequence,r.getLong("sender_id"),r.getString("sender_name"),r.getString("body"),r.getObject("created_at",LocalDateTime.class),r.getLong("sender_id")==actor),id,sequence).getFirst();}
    public void markRead(Long actor,long id,Read input){long last=participant(actor,id,true);if(input.sequence()==null||input.sequence()<0||input.sequence()>last)throw MarketplaceException.invalid("Mốc đã đọc không hợp lệ.");
        db.update("update private_participants set last_read_sequence=greatest(last_read_sequence,?) where conversation_id=? and user_id=?",input.sequence(),id,actor);
    }
}
