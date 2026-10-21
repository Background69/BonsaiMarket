package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.ai.AiException;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Semaphore;

@Component
public class AdminAiLimits {
    private record Window(long start,int count) {}
    private final Map<Long,Window> users=new HashMap<>();
    private final Semaphore slots=new Semaphore(2);
    private final Clock clock;
    private long globalStart;
    private int globalCount;
    public AdminAiLimits(Clock clock) { this.clock=clock;this.globalStart=clock.millis(); }
    public synchronized AutoCloseable enter(long admin) {
        long now=clock.millis(); users.entrySet().removeIf(e->now-e.getValue().start()>=60000);
        if(now-globalStart>=60000) {globalStart=now;globalCount=0;}
        var previous=users.get(admin);
        if(globalCount>=30 || previous!=null&&previous.count()>=6 || previous==null&&users.size()>=4096 || !slots.tryAcquire())
            throw new AiException(429,"ADMIN_AI_RATE_LIMIT","Trợ lý đang bận hoặc bạn đã gửi 6 yêu cầu trong một phút. Vui lòng thử lại sau.");
        globalCount++;users.put(admin,new Window(previous==null?now:previous.start(),previous==null?1:previous.count()+1));
        return slots::release;
    }
}
