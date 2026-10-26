package com.bonsaimarket.backend.notification;
import com.bonsaimarket.backend.auth.AuthSettings;
import com.bonsaimarket.backend.marketplace.MarketplaceAccess;
import jakarta.persistence.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.*;
import org.springframework.mail.javamail.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Reuses the existing JavaMailSender/config; SMTP acceptance is not recipient delivery. */
@Service @EnableScheduling
public class OrderMailDispatcher {
    private final EntityManager em;private final MarketplaceAccess access;private final ObjectProvider<JavaMailSender> mail;private final AuthSettings settings;private final TransactionTemplate tx;private final boolean enabled;
    public OrderMailDispatcher(EntityManager em,MarketplaceAccess access,ObjectProvider<JavaMailSender> mail,AuthSettings settings,PlatformTransactionManager manager,@Value("${orders.mail.enabled:false}") boolean enabled){this.em=em;this.access=access;this.mail=mail;this.settings=settings;this.enabled=enabled;tx=new TransactionTemplate(manager);tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
    private <T>T db(Supplier<T> action){return tx.execute(s->action.get());}
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void tick(){if(enabled)for(int i=0;i<5;i++)if(!deliverOnce())break;}
    public boolean deliverOnce(){
        var item=db(()->{
            em.createQuery("update OrderNotification n set n.status='UNKNOWN',n.errorCode='SEND_LEASE_EXPIRED',n.updatedAt=:now where n.status='SENDING' and n.updatedAt<:cutoff")
                .setParameter("now",access.now()).setParameter("cutoff",access.now().minusMinutes(10)).executeUpdate();
            var rows=em.createQuery("select n from OrderNotification n where n.status='PENDING' and n.nextAttemptAt<=:now order by n.createdAt,n.id",OrderNotification.class).setParameter("now",access.now()).setMaxResults(1).getResultList();
            if(rows.isEmpty())return null;var n=rows.getFirst();em.refresh(n,LockModeType.PESSIMISTIC_WRITE);if(!n.getStatus().equals("PENDING"))return null;
            if(n.getAttempts()>=8){n.setStatus("FAILED");return null;}n.setAttempts(n.getAttempts()+1);n.setStatus("SENDING");n.setUpdatedAt(access.now());em.flush();return n;
        });
        if(item==null)return false;
        String outcome="SMTP_ACCEPTED",error=null;
        try{
            var sender=mail.getIfAvailable();if(!settings.mailConfigured||sender==null){outcome="PENDING";error="SMTP_NOT_CONFIGURED";}
            else{var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,false,"UTF-8");helper.setValidateAddresses(true);helper.setFrom(settings.mailFrom);helper.setTo(item.getRecipient());helper.setSubject(item.getSubject());helper.setText(item.getBody(),false);sender.send(message);}
        }catch(MailAuthenticationException e){outcome="PENDING";error="SMTP_AUTH_FAILED";}
        catch(MailParseException e){outcome="FAILED";error="SMTP_MESSAGE_INVALID";}
        catch(Exception e){outcome="UNKNOWN";error="SMTP_SEND_UNCONFIRMED";} // Never log raw SMTP exceptions or blindly resend ambiguous DATA.
        final String result=outcome,code=error;
        db(()->{var n=em.find(OrderNotification.class,item.getId());em.refresh(n,LockModeType.PESSIMISTIC_WRITE);
            if(!n.getStatus().equals("SENDING"))return null;n.setStatus(result.equals("PENDING")&&n.getAttempts()>=8?"FAILED":result);n.setErrorCode(code);n.setUpdatedAt(access.now());n.setNextAttemptAt(access.now().plusSeconds(Math.min(3600,30L*(1L<<n.getAttempts()))));return null;});return true;
    }
}
