package com.bonsaimarket.backend.wallet;

import com.bonsaimarket.backend.marketplace.*;
import java.math.BigDecimal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Append-only double-entry non-monetary credits; no client balance setters or payment callbacks. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class WalletCredits {
    private final JdbcTemplate db;private final MarketplaceAccess access;
    public WalletCredits(JdbcTemplate db,MarketplaceAccess access){this.db=db;this.access=access;}
    public record Balance(String unit,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal available,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal reserved,String notice) {}
    public record Journal(String id,String type,String status,String reference,String description,String createdAt,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal availableDelta,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal reservedDelta) {}
    public record History(List<Journal> content,int page,long totalElements,int totalPages) {}
    private void lock(){db.queryForObject("select id from credit_control where id=1 for update",Integer.class);}
    private long account(Long owner){String key=owner==null?"TREASURY":"USER:"+owner;var found=db.query("select id from credit_accounts where account_key=?",(r,n)->r.getLong(1),key);if(!found.isEmpty())return found.getFirst();db.update("insert into credit_accounts(account_key,owner_id,available,reserved) values(?,?,0,0)",key,owner);return db.queryForObject("select id from credit_accounts where account_key=?",Long.class,key);}
    public static String digest(String text){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public static String key(String source){return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();}
    public static BigDecimal amount(BigDecimal amount){if(amount==null||amount.scale()>0&&amount.stripTrailingZeros().scale()>0||amount.compareTo(BigDecimal.ONE)<0||amount.compareTo(new BigDecimal("1000000"))>0)throw MarketplaceException.invalid("Số credit phải là số nguyên từ 1 đến 1.000.000.");return amount.setScale(0);}
    @Transactional(readOnly=true) public Balance balance(long actor){access.actor(actor);var values=db.query("select available,reserved from credit_accounts where owner_id=?",(r,n)->new BigDecimal[]{r.getBigDecimal(1),r.getBigDecimal(2)},actor);return new Balance("INTERNAL_CREDIT",values.isEmpty()?BigDecimal.ZERO:values.getFirst()[0],values.isEmpty()?BigDecimal.ZERO:values.getFirst()[1],"Credit nội bộ cho cạnh tranh banner; không phải tiền, không thanh toán đơn và không nạp/rút.");}
    @Transactional(readOnly=true) public History history(long actor,int page,String type,String status){
        access.actor(actor);MarketplaceAccess.page(page);if(type!=null&&!type.isEmpty()&&!Set.of("GRANT","RESERVE","RELEASE","CHARGE").contains(type))throw MarketplaceException.invalid("Loại giao dịch không hợp lệ.");if(status!=null&&!status.isEmpty()&&!Set.of("COMPLETED","PENDING","FAILED").contains(status))throw MarketplaceException.invalid("Trạng thái không hợp lệ.");
        String where=" from credit_journals j join credit_entries e on e.journal_id=j.id join credit_accounts a on a.id=e.account_id where a.owner_id=?";var args=new ArrayList<Object>();args.add(actor);if(type!=null&&!type.isEmpty()){where+=" and j.type=?";args.add(type);}if(status!=null&&!status.isEmpty()){where+=" and j.status=?";args.add(status);}
        long count=db.queryForObject("select count(*)"+where,Long.class,args.toArray());args.add(page*20);
        var rows=db.query("select j.*,e.available_delta,e.reserved_delta"+where+" order by j.created_at desc,j.id desc limit 20 offset ?",(r,n)->new Journal(r.getString("id"),r.getString("type"),r.getString("status"),r.getString("reference"),r.getString("description"),r.getTimestamp("created_at").toLocalDateTime()+"Z",r.getBigDecimal("available_delta"),r.getBigDecimal("reserved_delta")),args.toArray());return new History(rows,page,count,(int)((count+19)/20));
    }
    @Transactional(readOnly=true) public Journal detail(long actor,String id){access.actor(actor);return db.query("select j.*,e.available_delta,e.reserved_delta from credit_journals j join credit_entries e on e.journal_id=j.id join credit_accounts a on a.id=e.account_id where a.owner_id=? and j.id=?",(r,n)->new Journal(r.getString("id"),r.getString("type"),r.getString("status"),r.getString("reference"),r.getString("description"),r.getTimestamp("created_at").toLocalDateTime()+"Z",r.getBigDecimal("available_delta"),r.getBigDecimal("reserved_delta")),actor,id).stream().findFirst().orElseThrow(MarketplaceException::missing);}
    public Journal grant(long actor,long owner,BigDecimal value,UUID request,String reason){access.admin(actor);access.actor(owner);value=amount(value);String text=MarketplaceAccess.text(reason);if(text.length()<8||text.length()>1000)throw MarketplaceException.invalid("Cần lý do cấp credit từ 8 đến 1.000 ký tự.");String id=post(actor,owner,value,request.toString(),"GRANT","admin-grant:"+owner,text);return detail(owner,id);}
    public String reserve(long actor,BigDecimal value,String request,String reference){access.seller(actor);return post(actor,actor,amount(value),request,"RESERVE",reference,"Giữ credit cho lượt cạnh tranh banner");}
    public String release(long actor,BigDecimal value,String reference){return post(actor,actor,amount(value),key("release:"+reference),"RELEASE",reference,"Giải phóng credit đã giữ cho banner");}
    public String charge(long actor,BigDecimal value,String reference){return post(actor,actor,amount(value),key("charge:"+reference),"CHARGE",reference,"Sử dụng credit nội bộ cho vị trí banner đã thắng");}
    private String post(long actor,long owner,BigDecimal value,String request,String type,String reference,String description){
        UUID.fromString(request);lock();String fingerprint=digest(owner+"|"+value+"|"+type+"|"+reference+"|"+description);
        var previous=db.queryForList("select id,fingerprint from credit_journals where actor_id=? and request_key=?",actor,request);if(!previous.isEmpty()){if(!fingerprint.equals(previous.getFirst().get("fingerprint")))throw new MarketplaceException(409,"CREDIT_REQUEST_CONFLICT","Mã thao tác credit đã dùng cho yêu cầu khác.");return previous.getFirst().get("id").toString();}
        long user=account(owner),treasury=account(null);BigDecimal available=BigDecimal.ZERO,reserved=BigDecimal.ZERO,other=BigDecimal.ZERO;
        switch(type){case "GRANT"->{available=value;other=value.negate();}case "RESERVE"->{available=value.negate();reserved=value;}case "RELEASE"->{available=value;reserved=value.negate();}case "CHARGE"->{reserved=value.negate();other=value;}default->throw new IllegalStateException();}
        if(available.add(reserved).add(other).signum()!=0)throw new IllegalStateException("Unbalanced credit journal");
        int changed=db.update("update credit_accounts set available=available+?,reserved=reserved+? where id=? and available+?>=0 and reserved+?>=0",available,reserved,user,available,reserved);if(changed!=1)throw new MarketplaceException(409,"INSUFFICIENT_CREDITS","Số credit khả dụng hoặc đã giữ không đủ.");
        if(other.signum()!=0)db.update("update credit_accounts set available=available+? where id=?",other,treasury);
        String id=UUID.randomUUID().toString();db.update("insert into credit_journals(id,actor_id,request_key,fingerprint,type,status,reference,description,created_at) values(?,?,?,?,?,'COMPLETED',?,?,?)",id,actor,request,fingerprint,type,reference,description,access.now());db.update("insert into credit_entries(journal_id,account_id,available_delta,reserved_delta) values(?,?,?,?)",id,user,available,reserved);if(other.signum()!=0)db.update("insert into credit_entries(journal_id,account_id,available_delta,reserved_delta) values(?,?,?,0)",id,treasury,other);return id;
    }
    @Transactional(readOnly=true) public Object reconcile(long actor){access.admin(actor);return db.queryForList("select a.id,a.owner_id,a.available,a.reserved,coalesce(sum(e.available_delta),0) ledger_available,coalesce(sum(e.reserved_delta),0) ledger_reserved from credit_accounts a left join credit_entries e on e.account_id=a.id group by a.id,a.owner_id,a.available,a.reserved having a.available<>coalesce(sum(e.available_delta),0) or a.reserved<>coalesce(sum(e.reserved_delta),0)");}
}
