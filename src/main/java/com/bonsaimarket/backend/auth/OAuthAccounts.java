package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.time.*;
import java.util.*;

@Service
public class OAuthAccounts {
    public record External(String provider,String subject,String name,String email,boolean verified,long expiresAt) implements Serializable {}
    public record LinkIntent(Long userId,long version,String provider,long expiresAt) implements Serializable {}
    private final UserRepository users;
    private final IdentityRepository identities;
    private final AuthService auth;
    private final Clock clock;
    private final AuthSettings settings;
    public OAuthAccounts(UserRepository users,IdentityRepository identities,AuthService auth,Clock clock,AuthSettings settings) {
        this.users=users;this.identities=identities;this.auth=auth;this.clock=clock;this.settings=settings;
    }
    private void validate(External external) {
        if(!Set.of("google","facebook").contains(external.provider()) || external.subject()==null
            || !external.subject().matches("[\\x21-\\x7e]{1,255}") || external.expiresAt()<clock.millis())
            throw new AuthException(400,"OAUTH_INVALID_IDENTITY","Phiên xác thực mạng xã hội không hợp lệ hoặc đã hết hạn.");
    }
    @Transactional
    public User resolve(External external,LinkIntent link) {
        validate(external);
        var identity=identities.findByProviderAndProviderSubject(external.provider(),external.subject());
        if(link!=null) {
            if(link.expiresAt()<clock.millis() || !link.provider().equals(external.provider())) throw new AuthException(400,"OAUTH_LINK_EXPIRED","Yêu cầu liên kết đã hết hạn. Hãy xác nhận mật khẩu lại.");
            User target=users.lockById(link.userId()).orElseThrow(()->new AuthException(401,"SESSION_EXPIRED","Vui lòng đăng nhập lại."));
            auth.requireActive(target);
            if(target.getAuthVersion()!=link.version()) throw new AuthException(401,"SESSION_EXPIRED","Vui lòng đăng nhập lại.");
            if(identity.isPresent() && !identity.get().getUserId().equals(target.getId()))
                throw new AuthException(409,"OAUTH_ALREADY_LINKED","Tài khoản mạng xã hội đã liên kết với một tài khoản khác.");
            if(identity.isEmpty()) link(target,external);
            return target;
        }
        if(identity.isPresent()) {
            User user=users.lockById(identity.get().getUserId()).orElseThrow(); auth.requireActive(user); return user;
        }
        if(!external.verified() || external.email()==null) return null;
        String email=AuthService.emailKey(external.email());
        if(users.findByEmailKey(email).isPresent()) throw collision();
        User user=auth.newCustomer(safeName(external.name()),email,"{oauth}"+UUID.randomUUID(),true);
        users.saveAndFlush(user); link(user,external); return user;
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void complete(External external,AuthDtos.Complete request) {
        validate(external);
        settings.requireDelivery();
        String email=AuthService.emailKey(request.email());
        if(users.findByEmailKey(email).isPresent()) throw collision();
        if(identities.findByProviderAndProviderSubject(external.provider(),external.subject()).isPresent())
            throw new AuthException(409,"OAUTH_ALREADY_LINKED","Tài khoản này đã liên kết. Hãy đăng nhập lại qua nhà cung cấp và gửi lại mã nếu cần.");
        User user=auth.newCustomer(safeName(request.name()),email,"{oauth}"+UUID.randomUUID(),false);
        users.saveAndFlush(user); link(user,external); auth.issue(user,AuthService.VERIFY);
    }
    private void link(User user,External external) {
        if(identities.existsByUserIdAndProvider(user.getId(),external.provider())) throw new AuthException(409,"OAUTH_ALREADY_LINKED","Bạn đã liên kết một tài khoản của nhà cung cấp này.");
        OAuthIdentity identity=new OAuthIdentity(); identity.setUserId(user.getId()); identity.setProvider(external.provider());
        identity.setProviderSubject(external.subject()); identity.setCreatedAt(auth.now()); identities.saveAndFlush(identity);
    }
    private String safeName(String name) {
        if(name==null || name.strip().isBlank()) return "Người dùng BonsaiMarket";
        return name.strip().substring(0,Math.min(name.strip().length(),150));
    }
    private AuthException collision() {return new AuthException(409,"OAUTH_EMAIL_COLLISION","Email đã có tài khoản. Hãy đăng nhập bằng phương thức hiện có, rồi xác nhận mật khẩu để liên kết trong trang tài khoản.");}
}
