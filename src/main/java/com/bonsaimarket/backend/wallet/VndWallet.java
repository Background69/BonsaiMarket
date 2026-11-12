package com.bonsaimarket.backend.wallet;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.notification.InAppNotifications;
import com.bonsaimarket.backend.order.Order;
import com.bonsaimarket.backend.user.UserRole;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Simulated VND only. Global journal lock is deliberate for this bounded QA workload. */
@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class VndWallet {
    public static final String NOTICE="Chế độ thử nghiệm — Tiền mô phỏng, không có giá trị thanh toán thực tế.";
    public record Balance(boolean enabled,String mode,String currency,Long available,Long held,String updatedAt,String notice,Map<String,Object> bank) {}
    public record Transaction(String id,String type,String status,long amount,long signedAmount,String currency,Long orderId,String reference,String createdAt,String processedAt,Long balanceAfter,String explanation) {}
    public record History(List<Transaction> content,int page,long totalElements,int totalPages) {}
    private final JdbcTemplate db;private final MarketplaceAccess access;private final EntityManager em;private final VndWalletProvider provider;
    private final PasswordEncoder passwords;private final Environment environment;private final boolean enabled;
    private final com.bonsaimarket.backend.auth.AuthRateLimiter stepupLimiter;
    public VndWallet(JdbcTemplate db,MarketplaceAccess access,EntityManager em,SandboxVndWalletProvider provider,PasswordEncoder passwords,Environment environment,@Value("${wallet.sandbox.enabled:false}") boolean enabled,com.bonsaimarket.backend.auth.AuthRateLimiter stepupLimiter){this.db=db;this.access=access;this.em=em;this.provider=provider;this.passwords=passwords;this.environment=environment;this.enabled=enabled;this.stepupLimiter=stepupLimiter;}
    public boolean enabled(){
        var profiles=Set.of(environment.getActiveProfiles());
        if(!enabled||profiles.contains("prod")||profiles.contains("production")||!profiles.contains("wallet-sandbox"))return false;
        // An opt-in alone cannot turn a team/production database into a sandbox wallet.
        var source=Objects.requireNonNull(db.getDataSource());var c=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(source);
        try{
            String url=c.getMetaData().getURL(),catalog=c.getCatalog();
            return url.startsWith("jdbc:mysql:")&&catalog!=null&&catalog.startsWith("bm_wallet_qa_")||profiles.contains("wallet-test")&&url.startsWith("jdbc:h2:mem:vndwallet");
        }catch(java.sql.SQLException e){return false;}finally{org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(c,source);}
    }
    private void member(long actor){var u=access.actor(actor);if(!enabled())throw new MarketplaceException(409,"WALLET_DISABLED","Ví VND chỉ mở trên môi trường QA cô lập.");if(u.getRole()!=UserRole.CUSTOMER||db.queryForObject("select count(*) from vnd_wallet_qa_members where user_id=?",Long.class,actor)!=1)throw new MarketplaceException(403,"WALLET_INELIGIBLE","Tài khoản chưa được cấp quyền thử nghiệm ví VND.");}
    private void lock(){db.queryForObject("select id from vnd_wallet_control where id=1 for update",Integer.class);}
    private long account(String key,Long owner){var found=db.query("select id from vnd_wallet_accounts where account_key=?",(r,n)->r.getLong(1),key);if(!found.isEmpty())return found.getFirst();db.update("insert into vnd_wallet_accounts(account_key,owner_id) values(?,?)",key,owner);return db.queryForObject("select id from vnd_wallet_accounts where account_key=?",Long.class,key);}
    private long user(long actor){return account("BUYER:"+actor,actor);}
    private void stepUp(long actor,String password){try{stepupLimiter.check("VND_WALLET_STEP_UP","actor:"+actor,"actor:"+actor,5,5,300);}catch(com.bonsaimarket.backend.auth.AuthException e){throw new MarketplaceException(429,"WALLET_STEP_UP_LIMIT","Đã vượt giới hạn xác nhận mật khẩu. Thử lại sau 5 phút.");}var u=access.actor(actor);if(password==null||password.length()>200||!passwords.matches(password,u.getPasswordHash()))throw new MarketplaceException(403,"WALLET_STEP_UP_REQUIRED","Xác nhận bằng mật khẩu hiện tại của tài khoản QA. Tài khoản chỉ dùng OAuth chưa hỗ trợ thao tác này.");}
    private long amount(BigDecimal value){try{long n=value.longValueExact();if(n<1000||n>10000000)throw new ArithmeticException();return n;}catch(NullPointerException|ArithmeticException e){throw MarketplaceException.invalid("Số tiền phải là số nguyên từ 1.000 đến 10.000.000 VND.");}}
    private void key(String key){if(key==null||!key.matches("[A-Za-z0-9_-]{16,80}"))throw MarketplaceException.invalid("Cần mã chống ghi trùng hợp lệ.");}
    @Transactional(readOnly=true) public Balance balance(long actor){access.actor(actor);if(!enabled())return new Balance(false,"DISABLED","VND",null,null,null,NOTICE,Map.of());
        member(actor);var rows=db.queryForList("select available,held from vnd_wallet_accounts where owner_id=?",actor);
        return new Balance(true,"SANDBOX","VND",rows.isEmpty()?0:((Number)rows.getFirst().get("available")).longValue(),rows.isEmpty()?0:((Number)rows.getFirst().get("held")).longValue(),access.now()+"Z",NOTICE,bank(actor));}
    private Map<String,Object> bank(long actor){var rows=db.queryForList("select display_name,masked_number,status from vnd_wallet_banks where owner_id=?",actor);return rows.isEmpty()?Map.of():rows.getFirst();}
    public Object linkBank(long actor){member(actor);lock();if(bank(actor).isEmpty()){var b=provider.linkTestBank();db.update("insert into vnd_wallet_banks(owner_id,provider_reference,display_name,masked_number,status) values(?,?,?,?,?)",actor,b.reference(),b.name(),b.maskedNumber(),b.status());}return bank(actor);}
    private String create(long actor,String type,long amount,String key,Long order,String original){
        key(key);String fingerprint=WalletCredits.digest(type+"|"+amount+"|"+order+"|"+original);
        var old=db.queryForList("select id,fingerprint from vnd_wallet_transactions where owner_id=? and request_key=?",actor,key);
        if(!old.isEmpty()){if(!fingerprint.equals(old.getFirst().get("fingerprint")))throw new MarketplaceException(409,"WALLET_REQUEST_CONFLICT","Mã yêu cầu đã dùng cho nội dung khác.");return old.getFirst().get("id").toString();}
        if(db.queryForObject("select count(*) from vnd_wallet_transactions where owner_id=? and created_at>=?",Long.class,actor,access.now().minusMinutes(1))>=10)throw new MarketplaceException(429,"WALLET_RATE_LIMIT","Tối đa 10 yêu cầu ví mỗi phút.");
        long daily=db.queryForObject("select coalesce(sum(amount),0) from vnd_wallet_transactions where owner_id=? and type=? and status<>'FAILED' and created_at>=?",Long.class,actor,type,access.now().toLocalDate().atStartOfDay());
        if(daily+amount>50000000)throw new MarketplaceException(409,"WALLET_DAILY_LIMIT","Đã đạt hạn mức thử nghiệm 50.000.000 VND/ngày cho loại giao dịch này.");
        String id=UUID.randomUUID().toString();db.update("insert into vnd_wallet_transactions(id,owner_id,request_key,fingerprint,type,status,amount,order_id,original_id,created_at) values(?,?,?,?,?,'PENDING',?,?,?,?)",id,actor,key,fingerprint,type,amount,order,original,access.now());MarketplaceAudit.record(em,access,actor,"VND_WALLET_REQUESTED","VND_WALLET",actor,"Sandbox transaction "+id);return id;
    }
    public Transaction request(long actor,String type,BigDecimal value,String key,String password){member(actor);if(type==null||!Set.of("TOP_UP","WITHDRAWAL").contains(type))throw MarketplaceException.invalid("Loại yêu cầu không hỗ trợ.");key(key);long n=amount(value);lock();
        // A persisted replay never re-reserves funds. Credentials are never persisted.
        var existing=db.queryForList("select id from vnd_wallet_transactions where owner_id=? and request_key=?",actor,key);
        if(type.equals("WITHDRAWAL")&&existing.isEmpty()){stepUp(actor,password);if(bank(actor).isEmpty())throw new MarketplaceException(409,"WALLET_BANK_REQUIRED","Cần tài khoản ngân hàng mô phỏng trước khi rút thử nghiệm.");}
        String id=create(actor,type,n,key,null,null);if(existing.isEmpty()&&type.equals("WITHDRAWAL")){post(id,"RESERVE",user(actor),-n,n,null,0);notify(actor,id,"PENDING");}
        return detail(actor,id);
    }
    public Transaction simulate(long actor,String id,String outcome){member(actor);if(outcome==null||!Set.of("COMPLETED","FAILED").contains(outcome))throw MarketplaceException.invalid("Kết quả mô phỏng không hợp lệ.");lock();var t=detail(actor,id);if(!Set.of("TOP_UP","WITHDRAWAL").contains(t.type()))throw MarketplaceException.invalid("Không mô phỏng loại giao dịch này.");return confirm(actor,provider.simulate(id,t.amount(),outcome));}
    // Internal adapter boundary, deliberately NOT a public webhook/controller mapping.
    public Transaction confirm(long actor,VndWalletProvider.Confirmation event){member(actor);if(!provider.verify(event))throw new MarketplaceException(403,"WALLET_SIGNATURE_INVALID","Chữ ký xác nhận không hợp lệ.");lock();var t=detail(actor,event.transactionId());
        if(t.amount()!=event.amount()||!"VND".equals(event.currency()))throw new MarketplaceException(409,"WALLET_CONFIRMATION_MISMATCH","Xác nhận không khớp số tiền hoặc đơn vị.");
        if(!Set.of("TOP_UP","WITHDRAWAL").contains(t.type())||!Set.of("COMPLETED","FAILED").contains(event.outcome()))throw MarketplaceException.invalid("Xác nhận không hợp lệ.");
        if(!t.status().equals("PENDING")){if(!t.status().equals(event.outcome()))throw new MarketplaceException(409,"WALLET_FINAL_STATE","Giao dịch đã kết thúc với kết quả khác.");return t;}
        db.update("insert into vnd_wallet_provider_events(transaction_id,event_id,outcome,received_at) values(?,?,?,?)",t.id(),event.eventId(),event.outcome(),access.now());
        long n=t.amount();boolean completed=event.outcome().equals("COMPLETED");
        if(t.type().equals("TOP_UP")&&completed)post(t.id(),"CONFIRM",user(actor),n,0,account("SIMULATOR_CLEARING",null),-n);
        if(t.type().equals("WITHDRAWAL"))post(t.id(),completed?"CONFIRM":"RELEASE",user(actor),completed?0:n,-n,completed?account("SIMULATOR_CLEARING",null):null,completed?n:0);
        finish(t.id(),event.outcome(),"SIM:"+event.eventId());notify(actor,t.id(),event.outcome());return detail(actor,t.id());
    }
    private void finish(String id,String status,String ref){int changed=db.update("update vnd_wallet_transactions set status=?,provider_reference=?,processed_at=? where id=? and status='PENDING'",status,ref,access.now(),id);if(changed==1){long actor=db.queryForObject("select owner_id from vnd_wallet_transactions where id=?",Long.class,id);MarketplaceAudit.record(em,access,actor,"VND_WALLET_"+status,"VND_WALLET",actor,"Sandbox transaction "+id);}}
    private void post(String tx,String phase,long account,long available,long held,Long other,long opposite){
        if(Math.addExact(Math.addExact(available,held),opposite)!=0)throw new IllegalStateException("Unbalanced VND journal");
        String id=UUID.randomUUID().toString();db.update("insert into vnd_wallet_journals(id,transaction_id,phase,created_at) values(?,?,?,?)",id,tx,phase,access.now());
        entry(id,account,available,held);if(other!=null)entry(id,other,opposite,0);
    }
    private void entry(String journal,long account,long available,long held){
        int changed=db.update("update vnd_wallet_accounts set available=available+?,held=held+? where id=? and (owner_id is null or (available+?>=0 and held+?>=0))",available,held,account,available,held);
        if(changed!=1)throw new MarketplaceException(409,"WALLET_INSUFFICIENT_FUNDS","Số dư ví khả dụng không đủ.");
        db.update("insert into vnd_wallet_entries(journal_id,account_id,available_delta,held_delta,available_after,held_after) select ?,id,?,?,available,held from vnd_wallet_accounts where id=?",journal,available,held,account);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void pay(long actor,Order order,String key){member(actor);if(!order.getBuyer().getId().equals(actor))throw MarketplaceException.missing();long n=order.getTotal().longValueExact();if(n<1||n>10000000)throw MarketplaceException.invalid("Tổng đơn vượt hạn mức ví.");lock();String id=create(actor,"WALLET_PAYMENT",n,key,order.getId(),null);var t=detail(actor,id);if(t.status().equals("COMPLETED"))return;
        post(id,"PAYMENT",user(actor),-n,0,account("ORDER_OBLIGATIONS",null),n);finish(id,"COMPLETED",null);
        db.update("insert into vnd_wallet_obligations(order_id,seller_id,subtotal,shipping_fee,total,status) values(?,?,?,?,?,'SANDBOX_HELD')",order.getId(),order.getStore().getOwner().getId(),order.getSubtotal().longValueExact(),order.getShippingFee().longValueExact(),n);notify(actor,id,"COMPLETED");
    }
    public boolean refundable(Order order){return enabled()&&db.queryForObject("select count(*) from vnd_wallet_transactions t join vnd_wallet_obligations b on b.order_id=t.order_id where t.order_id=? and t.owner_id=? and t.type='WALLET_PAYMENT' and t.status='COMPLETED' and t.amount=? and b.status='SANDBOX_HELD'",Long.class,order.getId(),order.getBuyer().getId(),order.getTotal().longValueExact())==1;}
    @Transactional(propagation=Propagation.MANDATORY)
    public void refundCancelled(Order order){if(!"CANCELLED".equals(order.getStatus().name()))throw new IllegalStateException("Cancellation must be authoritative");long actor=order.getBuyer().getId();member(actor);lock();
        var payment=db.queryForList("select id,amount from vnd_wallet_transactions where order_id=? and owner_id=? and type='WALLET_PAYMENT' and status='COMPLETED'",order.getId(),actor);if(payment.size()!=1)throw new MarketplaceException(409,"REFUND_REVIEW_REQUIRED","Thiếu nguồn thanh toán ví sandbox phù hợp.");
        String original=payment.getFirst().get("id").toString();long n=((Number)payment.getFirst().get("amount")).longValue();if(n!=order.getTotal().longValueExact())throw new MarketplaceException(409,"REFUND_REVIEW_REQUIRED","Số tiền cần đối soát.");
        String id=create(actor,"REFUND",n,"REFUND_"+original,order.getId(),original);if(detail(actor,id).status().equals("COMPLETED"))return;
        if(!refundable(order))throw new MarketplaceException(409,"REFUND_REVIEW_REQUIRED","Nghĩa vụ thanh toán đã thay đổi.");
        notify(actor,id,"PENDING");post(id,"REFUND",user(actor),n,0,account("ORDER_OBLIGATIONS",null),-n);finish(id,"COMPLETED",null);db.update("update vnd_wallet_obligations set status='REFUNDED' where order_id=?",order.getId());notify(actor,id,"COMPLETED");
    }
    private void notify(long actor,String id,String state){InAppNotifications.wallet(em,access,actor,id,state);}
    private Transaction view(Map<String,Object> r,long actor){String id=r.get("id").toString(),type=r.get("type").toString(),state=r.get("status").toString();long n=((Number)r.get("amount")).longValue();
        Long after=null;if(state.equals("COMPLETED")){var balances=db.query("select e.available_after from vnd_wallet_entries e join vnd_wallet_accounts a on a.id=e.account_id join vnd_wallet_journals j on j.id=e.journal_id where j.transaction_id=? and a.owner_id=? and j.phase<>'RESERVE'",(s,i)->s.getLong(1),id,actor);if(balances.size()==1)after=balances.getFirst();}
        return new Transaction(id,type,state,n,Set.of("WITHDRAWAL","WALLET_PAYMENT").contains(type)?-n:n,"VND",r.get("order_id")==null?null:((Number)r.get("order_id")).longValue(),Objects.toString(r.get("provider_reference"),null),r.get("created_at").toString().replace(' ','T')+"Z",r.get("processed_at")==null?null:r.get("processed_at").toString().replace(' ','T')+"Z",after,NOTICE);
    }
    @Transactional(readOnly=true) public Transaction detail(long actor,String id){member(actor);return db.queryForList("select * from vnd_wallet_transactions where id=? and owner_id=?",id,actor).stream().findFirst().map(r->view(r,actor)).orElseThrow(MarketplaceException::missing);}
    @Transactional(readOnly=true) public Object recovery(long actor,String key){member(actor);return Collections.singletonMap("transaction",db.queryForList("select * from vnd_wallet_transactions where owner_id=? and request_key=?",actor,key).stream().findFirst().map(r->view(r,actor)).orElse(null));}
    @Transactional(readOnly=true) public History history(long actor,int page,String type,String status,LocalDate from,LocalDate to){member(actor);MarketplaceAccess.page(page);
        if(!type.isEmpty()&&!Set.of("TOP_UP","WITHDRAWAL","WALLET_PAYMENT","REFUND","REVERSAL").contains(type)||!status.isEmpty()&&!Set.of("PENDING","COMPLETED","FAILED","CANCELLED","REVERSED").contains(status)||from!=null&&to!=null&&from.isAfter(to))throw MarketplaceException.invalid("Bộ lọc lịch sử không hợp lệ.");
        String where=" where owner_id=?";var args=new ArrayList<Object>();args.add(actor);if(!type.isEmpty()){where+=" and type=?";args.add(type);}if(!status.isEmpty()){where+=" and status=?";args.add(status);}if(from!=null){where+=" and created_at>=?";args.add(from.atStartOfDay());}if(to!=null){where+=" and created_at<?";args.add(to.plusDays(1).atStartOfDay());}
        long total=db.queryForObject("select count(*) from vnd_wallet_transactions"+where,Long.class,args.toArray());args.add(page*10);
        return new History(db.queryForList("select * from vnd_wallet_transactions"+where+" order by created_at desc,id desc limit 10 offset ?",args.toArray()).stream().map(r->view(r,actor)).toList(),page,total,(int)((total+9)/10));
    }
    @Transactional(readOnly=true) public Object reconcile(long actor){access.admin(actor);if(!enabled())throw new MarketplaceException(409,"WALLET_DISABLED","Sandbox disabled");
        return Map.of("mode","SANDBOX",
            "balanceDifferences",db.queryForList("select a.id from vnd_wallet_accounts a left join vnd_wallet_entries e on e.account_id=a.id group by a.id,a.available,a.held having a.available<>coalesce(sum(e.available_delta),0) or a.held<>coalesce(sum(e.held_delta),0)"),
            "unbalancedJournals",db.queryForList("select journal_id from vnd_wallet_entries group by journal_id having sum(available_delta+held_delta)<>0"),
            "obligationDifferences",db.queryForList("select b.order_id from vnd_wallet_obligations b join orders o on o.id=b.order_id where b.total<>o.total or b.subtotal<>o.subtotal or b.shipping_fee<>o.shipping_fee"),
            "obligationLedgerDifferences",db.queryForList("select a.id from vnd_wallet_accounts a where a.account_key='ORDER_OBLIGATIONS' and a.available<>(select coalesce(sum(b.total),0) from vnd_wallet_obligations b where b.status='SANDBOX_HELD')"),
            "paymentDifferences",db.queryForList("select t.id from vnd_wallet_transactions t left join order_payments p on p.order_id=t.order_id where t.type='WALLET_PAYMENT' and t.status='COMPLETED' and (p.order_id is null or p.method<>'WALLET_SANDBOX' or p.amount_due<>t.amount or p.status not in ('SANDBOX_PAID','SANDBOX_REFUNDED'))"),
            "providerDifferences",db.queryForList("select t.id from vnd_wallet_transactions t left join vnd_wallet_provider_events e on e.transaction_id=t.id where t.type in ('TOP_UP','WITHDRAWAL') and t.status in ('COMPLETED','FAILED') and (e.transaction_id is null or e.outcome<>t.status)"),
            "pending",db.queryForObject("select count(*) from vnd_wallet_transactions where status='PENDING'",Long.class));
    }
}
