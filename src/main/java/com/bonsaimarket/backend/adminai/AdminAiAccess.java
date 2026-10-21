package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.auth.*;
import com.bonsaimarket.backend.user.UserRole;
import com.bonsaimarket.backend.ai.AiException;
import org.springframework.stereotype.Component;

@Component
public class AdminAiAccess {
    private final AuthService auth;
    public AdminAiAccess(AuthService auth) { this.auth = auth; }
    public long requireAdmin(AuthPrincipal principal) {
        if (principal == null) throw new AiException(401, "SESSION_EXPIRED", "Vui lòng đăng nhập lại.");
        var user = auth.current(principal);
        if (user.getRole() != UserRole.ADMIN) throw new AiException(403, "FORBIDDEN", "Chỉ quản trị viên được dùng trợ lý này.");
        return user.getId();
    }
}
