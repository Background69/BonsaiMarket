package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

@Component
public class OtpMac {
    private final AuthSettings settings;
    private final SecureRandom random=new SecureRandom();
    public OtpMac(AuthSettings settings) { this.settings=settings; }
    public String generate() { return String.format(java.util.Locale.ROOT,"%06d",random.nextInt(1_000_000)); }
    public String hash(AuthChallenge challenge,String code) {
        if (settings.otpKey.length<32) throw new AuthException(503,"AUTH_NOT_CONFIGURED","Dịch vụ xác thực email chưa được cấu hình.");
        try {
            Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(settings.otpKey,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((challenge.getUserId()+":"+challenge.getPurpose()+":"+challenge.getNonce()+":"+code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("HMAC unavailable",e); }
    }
    public boolean matches(AuthChallenge challenge,String code) {
        return MessageDigest.isEqual(hash(challenge,code).getBytes(StandardCharsets.US_ASCII),challenge.getCodeMac().getBytes(StandardCharsets.US_ASCII));
    }
}
