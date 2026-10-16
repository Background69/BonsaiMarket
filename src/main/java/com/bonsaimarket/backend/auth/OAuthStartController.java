package com.bonsaimarket.backend.auth;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class OAuthStartController {
    private final AuthSettings settings;
    public OAuthStartController(AuthSettings settings) {this.settings=settings;}
    @GetMapping("/oauth2/authorization/{provider}") public void unavailable() {
        throw new AuthException(503,"OAUTH_NOT_CONFIGURED","Nhà cung cấp đăng nhập chưa được cấu hình.");
    }
    @GetMapping("/login/oauth2/code/{provider}") public RedirectView invalidCallback() {
        return new RedirectView(settings.frontendOrigin+"/login?authError=OAUTH_FAILED");
    }
}
