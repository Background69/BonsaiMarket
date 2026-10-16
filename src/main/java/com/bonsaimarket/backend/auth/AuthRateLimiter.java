package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class AuthRateLimiter {
    private record Window(long until,int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final Clock clock;
    public AuthRateLimiter(Clock clock) {this.clock=clock;}
    public synchronized void check(String operation,String email,String ip,int accountLimit,int ipLimit,int seconds) {
        long now=clock.millis();
        windows.entrySet().removeIf(e->e.getValue().until<=now);
        if(windows.size()>20000) throw limited(seconds);
        String account=operation+":account:"+digest(email), peer=operation+":ip:"+digest(ip);
        Window a=windows.get(account), p=windows.get(peer);
        if((a!=null&&a.count>=accountLimit)||(p!=null&&p.count>=ipLimit)) throw limited(seconds);
        windows.put(account,new Window(a==null?now+seconds*1000L:a.until,a==null?1:a.count+1));
        windows.put(peer,new Window(p==null?now+seconds*1000L:p.until,p==null?1:p.count+1));
    }
    private AuthException limited(int seconds) {return new AuthException(429,"RATE_LIMITED","Bạn thao tác quá nhanh. Vui lòng thử lại sau.",seconds);}
    private String digest(String value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e) {throw new IllegalStateException(e);}
    }
}
