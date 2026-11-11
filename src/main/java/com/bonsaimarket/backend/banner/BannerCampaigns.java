package com.bonsaimarket.backend.banner;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.*;
import com.bonsaimarket.backend.wallet.WalletCredits;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class BannerCampaigns {
    private final JdbcTemplate db;private final MarketplaceAccess access;private final EntityManager em;private final WalletCredits wallet;
    public BannerCampaigns(JdbcTemplate db,MarketplaceAccess access,EntityManager em,WalletCredits wallet){this.db=db;this.access=access;this.em=em;this.wallet=wallet;}
    public record Create(@NotNull @Positive Long slotId,@NotBlank @Size(max=180) String title,@NotNull Instant biddingEndsAt,@NotNull Instant startsAt,@NotNull Instant endsAt,@NotNull BigDecimal minimumBid,@NotNull UUID requestKey) {}
    public record BidWrite(@NotNull @Positive Long productId,@NotNull BigDecimal credits,@NotNull UUID requestKey) {}
    public record Decision(@NotNull @Min(0) @tools.jackson.databind.annotation.JsonDeserialize(using=com.bonsaimarket.backend.media.GalleryRevisionDeserializer.class) Long revision,@NotBlank @Size(min=8,max=1000) String reason) {}
    public record SlotWrite(@NotBlank @Size(max=120) String name) {}
    public record Campaign(long id,long slotId,String title,String slotName,LocalDateTime biddingEndsAt,LocalDateTime startsAt,LocalDateTime endsAt,BigDecimal minimumBid,String status,long revision,Long winnerBidId,long createdBy) {}
    public record Bid(long id,long campaignId,long sellerId,long productId,String productName,BigDecimal amount,String status,boolean approved,LocalDateTime createdAt,String requestKey,String fingerprint) {}
    private final org.springframework.jdbc.core.RowMapper<Campaign> campaignMapper=(r,n)->new Campaign(r.getLong("id"),r.getLong("slot_id"),r.getString("title"),r.getString("slot_name"),r.getTimestamp("bidding_ends_at").toLocalDateTime(),r.getTimestamp("starts_at").toLocalDateTime(),r.getTimestamp("ends_at").toLocalDateTime(),r.getBigDecimal("minimum_bid"),r.getString("status"),r.getLong("revision"),(Long)r.getObject("winner_bid_id"),r.getLong("created_by"));
    private final org.springframework.jdbc.core.RowMapper<Bid> bidMapper=(r,n)->new Bid(r.getLong("id"),r.getLong("campaign_id"),r.getLong("seller_id"),r.getLong("product_id"),r.getString("product_name"),r.getBigDecimal("amount"),r.getString("status"),r.getBoolean("approved"),r.getTimestamp("created_at").toLocalDateTime(),r.getString("request_key"),r.getString("fingerprint"));
    private static final String CAMPAIGN="select c.*,s.name slot_name from banner_campaigns c join banner_slots s on s.id=c.slot_id";
    private static final String BIDS="select b.*,p.name product_name from banner_bids b join products p on p.id=b.product_id";
    private Campaign campaign(long id,boolean lock){if(lock){var ids=db.queryForList("select id from banner_campaigns where id=? for update",Long.class,id);if(ids.isEmpty())throw MarketplaceException.missing();}return db.query(CAMPAIGN+" where c.id=?",campaignMapper,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    private List<Bid> bids(long id){return db.query(BIDS+" where b.campaign_id=? order by b.amount desc,b.created_at,b.id",bidMapper,id);}
    private void audit(long actor,Long campaign,String action,String reason){db.update("insert into banner_audit(campaign_id,actor_id,action,reason,created_at) values(?,?,?,?,?)",campaign,actor,action,reason,access.now());}
    private boolean eligible(long product,long seller){return db.queryForObject("select count(*) from products p join stores s on s.id=p.store_id join users u on u.id=s.owner_user_id join categories c on c.id=p.category_id where p.id=? and s.owner_user_id=? and p.status='ACTIVE' and p.stock>0 and s.status='ACTIVE' and u.is_active=true and u.role in ('SELLER','ADMIN') and c.is_active=true",Long.class,product,seller)==1;}
    @Transactional(readOnly=true) public Object slots(long actor){access.seller(actor);return db.queryForList("select id,name,enabled,revision from banner_slots order by id");}
    public Object createSlot(long actor,String name){access.admin(actor);String text=MarketplaceAccess.text(name);if(text.isBlank()||text.length()>120)throw MarketplaceException.invalid("Tên vị trí từ 1 đến 120 ký tự.");if(db.queryForObject("select count(*) from banner_slots where name=?",Long.class,text)>0)throw new MarketplaceException(409,"SLOT_EXISTS","Vị trí đã tồn tại.");db.update("insert into banner_slots(name,enabled,revision) values(?,true,0)",text);audit(actor,null,"SLOT_CREATE","Tạo vị trí: "+text);return slots(actor);}
    public Object create(long actor,Create body){
        access.admin(actor);access.lock(com.bonsaimarket.backend.user.User.class,actor);BigDecimal min=WalletCredits.amount(body.minimumBid());var deadline=LocalDateTime.ofInstant(body.biddingEndsAt(),ZoneOffset.UTC);var start=LocalDateTime.ofInstant(body.startsAt(),ZoneOffset.UTC);var end=LocalDateTime.ofInstant(body.endsAt(),ZoneOffset.UTC);
        String fingerprint=WalletCredits.digest(body.slotId()+"|"+body.title().strip()+"|"+deadline+"|"+start+"|"+end+"|"+min);
        var previous=db.queryForList("select id,request_fingerprint from banner_campaigns where created_by=? and request_key=?",actor,body.requestKey().toString());if(!previous.isEmpty()){if(!fingerprint.equals(previous.getFirst().get("request_fingerprint")))throw new MarketplaceException(409,"CAMPAIGN_REQUEST_CONFLICT","Mã tạo chiến dịch đã được dùng.");return detail(actor,((Number)previous.getFirst().get("id")).longValue(),true);}
        if(!deadline.isAfter(access.now())||deadline.isAfter(start)||!start.isBefore(end)||end.isAfter(start.plusDays(30)))throw MarketplaceException.invalid("Hạn bid phải ở tương lai, không sau giờ bắt đầu; chiến dịch dài tối đa 30 ngày.");
        var slot=db.queryForList("select id from banner_slots where id=? and enabled=true for update",Long.class,body.slotId());if(slot.isEmpty())throw MarketplaceException.missing();
        if(db.queryForObject("select count(*) from banner_campaigns where slot_id=? and status<>'CANCELLED' and starts_at<? and ends_at>?",Long.class,body.slotId(),end,start)>0)throw new MarketplaceException(409,"SLOT_OVERLAP","Vị trí đã có lịch chồng lấn.");
        db.update("insert into banner_campaigns(slot_id,title,bidding_ends_at,starts_at,ends_at,minimum_bid,status,revision,created_by,request_key,request_fingerprint) values(?,?,?,?,?,?,'OPEN',0,?,?,?)",body.slotId(),body.title().strip(),deadline,start,end,min,actor,body.requestKey().toString(),fingerprint);
        long id=db.queryForObject("select id from banner_campaigns where created_by=? and request_key=?",Long.class,actor,body.requestKey().toString());audit(actor,id,"CAMPAIGN_CREATE","Tạo chiến dịch dùng credit nội bộ");return detail(actor,id,true);
    }
    @Transactional(readOnly=true) public Object list(long actor,int page,boolean admin){if(admin)access.admin(actor);else access.seller(actor);MarketplaceAccess.page(page);long count=db.queryForObject("select count(*) from banner_campaigns",Long.class);return Map.of("content",db.query(CAMPAIGN+" order by c.id desc limit 20 offset ?",campaignMapper,page*20),"page",page,"totalElements",count,"totalPages",(count+19)/20,"rules","Mỗi người bán một bid cố định mỗi chiến dịch. Cao nhất thắng; bằng điểm ưu tiên thời gian trước, rồi mã bid nhỏ hơn. Chỉ bid được Admin duyệt và sản phẩm còn đủ điều kiện. Giữ credit đến khi kết thúc; bên thua/hủy được giải phóng. Credit không phải tiền thật.");}
    @Transactional(readOnly=true) public Object detail(long actor,long id,boolean admin){if(admin)access.admin(actor);else access.seller(actor);var c=campaign(id,false);var all=bids(id);return Map.of("campaign",c,"bids",admin?all:all.stream().filter(b->b.sellerId()==actor).toList(),"ranking",all.stream().filter(b->b.approved()&&Set.of("RESERVED","WON").contains(b.status())&&eligible(b.productId(),b.sellerId())).map(b->Map.of("productName",b.productName(),"credits",b.amount(),"own",b.sellerId()==actor)).toList(),"audit",db.queryForList("select action,reason,created_at from banner_audit where campaign_id=? order by id",id));}
    public Object bid(long actor,long id,BidWrite body){
        access.seller(actor);var c=campaign(id,true);BigDecimal amount=WalletCredits.amount(body.credits());String digest=WalletCredits.digest(id+"|"+body.productId()+"|"+amount);
        var prior=db.query(BIDS+" where b.seller_id=? and b.request_key=?",bidMapper,actor,body.requestKey().toString());if(!prior.isEmpty()){if(!digest.equals(prior.getFirst().fingerprint()))throw new MarketplaceException(409,"BID_REQUEST_CONFLICT","Mã bid đã dùng cho yêu cầu khác.");return detail(actor,id,false);}
        if(!c.status().equals("OPEN")||!access.now().isBefore(c.biddingEndsAt()))throw new MarketplaceException(409,"BIDDING_CLOSED","Đã hết thời gian nhận bid.");
        if(amount.compareTo(c.minimumBid())<0)throw MarketplaceException.invalid("Bid thấp hơn mức tối thiểu.");
        if(!eligible(body.productId(),actor))throw MarketplaceException.missing();
        if(db.queryForObject("select count(*) from banner_bids where campaign_id=? and seller_id=?",Long.class,id,actor)>0)throw new MarketplaceException(409,"BID_EXISTS","Mỗi người bán chỉ có một bid trong chiến dịch.");
        wallet.reserve(actor,amount,body.requestKey().toString(),"campaign:"+id+":seller:"+actor);
        db.update("insert into banner_bids(campaign_id,seller_id,product_id,amount,status,approved,request_key,fingerprint,created_at) values(?,?,?,?,'RESERVED',false,?,?,?)",id,actor,body.productId(),amount,body.requestKey().toString(),digest,access.now());
        db.update("update banner_campaigns set revision=revision+1 where id=?",id);audit(actor,id,"BID_SUBMIT","Gửi bid và giữ credit nội bộ");return detail(actor,id,false);
    }
    private String reference(Bid b){return "campaign:"+b.campaignId()+":seller:"+b.sellerId();}
    private void release(Bid b,String status){if(Set.of("RESERVED","WON").contains(b.status())){wallet.release(b.sellerId(),b.amount(),reference(b));db.update("update banner_bids set status=? where id=?",status,b.id());}}
    public Object withdraw(long actor,long id,long bidId){access.seller(actor);var c=campaign(id,true);var bid=bids(id).stream().filter(b->b.id()==bidId&&b.sellerId()==actor).findFirst().orElseThrow(MarketplaceException::missing);if(bid.status().equals("WITHDRAWN"))return detail(actor,id,false);if(!c.status().equals("OPEN")||!access.now().isBefore(c.biddingEndsAt())||!bid.status().equals("RESERVED"))throw new MarketplaceException(409,"BID_LOCKED","Không còn được rút bid.");release(bid,"WITHDRAWN");db.update("update banner_campaigns set revision=revision+1 where id=?",id);audit(actor,id,"BID_WITHDRAW","Người bán rút bid, giải phóng credit");return detail(actor,id,false);}
    public Object moderate(long actor,long id,long bidId,boolean approve,Decision decision){access.admin(actor);var c=campaign(id,true);MarketplaceAccess.revision(c.revision(),decision.revision());if(!c.status().equals("OPEN"))throw new MarketplaceException(409,"CAMPAIGN_LOCKED","Chiến dịch không còn mở.");var b=bids(id).stream().filter(x->x.id()==bidId).findFirst().orElseThrow(MarketplaceException::missing);if(!b.status().equals("RESERVED"))throw new MarketplaceException(409,"BID_LOCKED","Bid đã kết thúc.");if(approve){if(!eligible(b.productId(),b.sellerId()))throw new MarketplaceException(409,"PRODUCT_INELIGIBLE","Sản phẩm không còn đủ điều kiện.");db.update("update banner_bids set approved=true where id=?",bidId);}else release(b,"REJECTED");db.update("update banner_campaigns set revision=revision+1 where id=?",id);audit(actor,id,approve?"BID_APPROVE":"BID_REJECT",decision.reason());return detail(actor,id,true);}
    public Object settle(long actor,long id,Decision decision){
        access.admin(actor);var c=campaign(id,true);if(c.status().equals("SCHEDULED"))return detail(actor,id,true);MarketplaceAccess.revision(c.revision(),decision.revision());
        if(!c.status().equals("OPEN")||access.now().isBefore(c.biddingEndsAt())||!access.now().isBefore(c.endsAt()))throw new MarketplaceException(409,"SETTLEMENT_UNAVAILABLE","Chỉ chốt sau hạn bid và trước khi chiến dịch kết thúc.");
        var bids=bids(id);var winner=bids.stream().filter(b->b.status().equals("RESERVED")&&b.approved()&&eligible(b.productId(),b.sellerId())).findFirst();
        for(var b:bids)if(winner.isPresent()&&b.id()==winner.get().id())db.update("update banner_bids set status='WON' where id=?",b.id());else release(b,"LOST");
        db.update("update banner_campaigns set status=?,winner_bid_id=?,revision=revision+1 where id=?",winner.isPresent()?"SCHEDULED":"CANCELLED",winner.map(Bid::id).orElse(null),id);audit(actor,id,"CAMPAIGN_SETTLE",decision.reason());return detail(actor,id,true);
    }
    public Object cancel(long actor,long id,Decision decision){access.admin(actor);var c=campaign(id,true);if(c.status().equals("CANCELLED"))return detail(actor,id,true);MarketplaceAccess.revision(c.revision(),decision.revision());if(c.status().equals("EXPIRED"))throw new MarketplaceException(409,"CAMPAIGN_EXPIRED","Chiến dịch đã kết thúc.");for(var b:bids(id))release(b,"RELEASED");db.update("update banner_campaigns set status='CANCELLED',revision=revision+1 where id=?",id);audit(actor,id,"CAMPAIGN_CANCEL",decision.reason());return detail(actor,id,true);}
    public void expireDue(){
        for(long id:db.queryForList("select id from banner_campaigns where ends_at<=? and status in ('OPEN','SCHEDULED') order by id limit 20",Long.class,access.now())){
            var c=campaign(id,true);if(!Set.of("OPEN","SCHEDULED").contains(c.status()))continue;
            for(var b:bids(id))if(b.status().equals("WON")&&db.queryForObject("select enabled from banner_slots where id=?",Boolean.class,c.slotId())&&eligible(b.productId(),b.sellerId()))wallet.charge(b.sellerId(),b.amount(),reference(b));else release(b,"RELEASED");
            db.update("update banner_campaigns set status='EXPIRED',revision=revision+1 where id=?",id);audit(c.createdBy(),id,"CAMPAIGN_EXPIRE","Kết thúc theo lịch UTC; thu credit nội bộ của winner đủ điều kiện, giải phóng các khoản còn lại");
        }
    }
    public void expire(long actor){access.admin(actor);expireDue();}
    @Transactional(readOnly=true) public Object winners(){return db.queryForList("select c.id campaignId,c.title,s.name slotName,p.id productId,p.name,p.price,p.stock,p.status,p.image_url imageUrl,t.id storeId,t.name storeName,c.starts_at startsAt,c.ends_at endsAt from banner_campaigns c join banner_slots s on s.id=c.slot_id join banner_bids b on b.id=c.winner_bid_id join products p on p.id=b.product_id join stores t on t.id=p.store_id join users u on u.id=t.owner_user_id join categories g on g.id=p.category_id where c.status='SCHEDULED' and s.enabled=true and b.status='WON' and b.approved=true and c.starts_at<=? and c.ends_at>? and p.status='ACTIVE' and p.stock>0 and t.status='ACTIVE' and u.is_active=true and u.role in ('SELLER','ADMIN') and g.is_active=true order by s.id,c.id",access.now(),access.now());}
}
