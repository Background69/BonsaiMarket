package com.bonsaimarket.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Base64;

@Component
public class AuthSettings {
    public final byte[] otpKey;
    public final String mailFrom, frontendOrigin, publicOrigin;
    public final boolean mailConfigured;
    public AuthSettings(@Value("${auth.otp-key:}") String key, @Value("${auth.mail-from:}") String mailFrom,
        @Value("${spring.mail.host:}") String mailHost, @Value("${auth.frontend-origin:http://localhost:5173}") String frontendOrigin,
        @Value("${auth.public-origin:http://localhost:5173}") String publicOrigin) {
        byte[] decoded;
        try { decoded = key.isBlank()?new byte[0]:Base64.getDecoder().decode(key); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("AUTH_OTP_HMAC_KEY must be base64."); }
        if (decoded.length>0 && decoded.length<32) throw new IllegalStateException("AUTH_OTP_HMAC_KEY needs at least 32 random bytes.");
        this.otpKey=decoded; this.mailFrom=mailFrom; this.mailConfigured=!mailHost.isBlank()&&!mailFrom.isBlank();
        this.frontendOrigin=origin(frontendOrigin); this.publicOrigin=origin(publicOrigin);
    }
    private String origin(String value) {
        var uri=java.net.URI.create(value);
        if (!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null
            || uri.getRawUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null
            || (uri.getPath()!=null&&!uri.getPath().isEmpty()&&!uri.getPath().equals("/")))
            throw new IllegalStateException("Auth origin must be an absolute HTTP(S) origin without a path.");
        if(uri.getScheme().equals("http")&&!java.util.Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost()))
            throw new IllegalStateException("Production auth origins require HTTPS.");
        return value.replaceAll("/$", "");
    }
    public void requireDelivery() {
        if (otpKey.length<32 || !mailConfigured)
            throw new AuthException(503,"AUTH_NOT_CONFIGURED","Dịch vụ xác thực email chưa được cấu hình. Vui lòng thử lại sau.");
    }
}
