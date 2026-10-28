package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.*;

@Service
public class PasswordResetRequests {
    private final Executor executor;
    private final AuthService auth;
    private final AuthSettings settings;
    public PasswordResetRequests(@Qualifier("authMailExecutor") Executor executor,AuthService auth,AuthSettings settings) {
        this.executor=executor;this.auth=auth;this.settings=settings;
    }
    public void request(String email) {
        settings.requireDelivery(); String canonical=AuthService.emailKey(email);
        try {
            // Phản hồi công khai không chờ tra cứu tài khoản hoặc SMTP, tránh dò tài khoản qua thời gian trả lời.
            executor.execute(()-> {try {auth.forgot(canonical);} catch(Exception ignored) { /* Không ghi danh tính, mã hoặc exception email vào log. */ }});
        } catch(RejectedExecutionException e) {throw new AuthException(503,"AUTH_BUSY","Dịch vụ đang bận. Vui lòng thử lại sau.");}
    }
}
