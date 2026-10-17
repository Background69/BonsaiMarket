package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.user.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.*;

/** DB-backed per-actor serialization and throttling shared only by discussion content. */
@Component
public class ContentAccess {
    public final MarketplaceAccess access;
    private final JdbcTemplate db;
    public ContentAccess(MarketplaceAccess access,JdbcTemplate db){this.access=access;this.db=db;}
    public User writer(Long id){if(id==null)throw new MarketplaceException(401,"SESSION_EXPIRED","Vui lòng đăng nhập.");access.lock(User.class,id);return access.actor(id);}
    public void owner(User actor,long author){if(!actor.getId().equals(author)&&actor.getRole()!=UserRole.ADMIN)throw new MarketplaceException(403,"FORBIDDEN","Bạn không có quyền sửa nội dung này.");}
    public static String text(String value,int max){
        String clean=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFC).strip();
        if(clean.isBlank() || clean.length()>max || clean.chars().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw MarketplaceException.invalid("Nội dung phải từ 1 đến "+max+" ký tự.");
        return clean;
    }
    public void throttle(String table,Long actor,int limit,String body,String scope,Object target){
        // table/scope are internal constants, never request inputs. Removed rows still count.
        var since=access.now().minusHours(1);
        if(db.queryForObject("select count(*) from "+table+" where author_id=? and created_at>=?",Long.class,actor,since)>=limit)
            throw new MarketplaceException(429,"CONTENT_RATE_LIMIT","Bạn gửi nội dung quá nhanh. Vui lòng thử lại sau.");
        var args=new ArrayList<Object>(List.of(actor,access.now().minusMinutes(10),body));
        if(target!=null)args.add(target);
        if(db.queryForObject("select count(*) from "+table+" where author_id=? and created_at>=? and body=?"+(target==null?"":" and "+scope+"=?"),Long.class,args.toArray())>0)
            throw new MarketplaceException(409,"CONTENT_DUPLICATE","Nội dung này vừa được gửi. Vui lòng tải lại trước khi gửi tiếp.");
    }
    public static int page(int page){return MarketplaceAccess.page(page);}
    public static long revision(Long revision){if(revision==null||revision<0)throw MarketplaceException.invalid("Cần phiên bản nội dung hiện tại.");return revision;}
    public record ContentPage<T>(List<T> content,int page,int size,long totalElements,int totalPages) {
        public static <T> ContentPage<T> of(List<T> content,int page,long total){return new ContentPage<>(content,page,10,total,(int)((total+9)/10));}
    }
}
