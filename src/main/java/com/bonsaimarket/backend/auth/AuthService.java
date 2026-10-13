package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {
    public static final String VERIFY="VERIFY_EMAIL", RESET="RESET_PASSWORD";
    private final UserRepository users;
    private final ChallengeRepository challenges;
    private final PasswordEncoder passwords;
    private final OtpMac mac;
    private final AuthMailSender mail;
    private final AuthSettings settings;
    private final Clock clock;
    private final String dummyHash;
    public AuthService(UserRepository users,ChallengeRepository challenges,PasswordEncoder passwords,
        OtpMac mac,AuthMailSender mail,AuthSettings settings,Clock clock) {
        this.users=users; this.challenges=challenges; this.passwords=passwords; this.mac=mac;
        this.mail=mail; this.settings=settings; this.clock=clock;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public LocalDateTime now() {return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    public static String emailKey(String email) {
        String key=email==null?"":email.strip().toLowerCase(Locale.ROOT);
        // ASCII addresses avoid MySQL accent-collation aliases and have one consistent canonical form.
        if(key.length()>255 || !key.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}"))
            throw new AuthException(400,"INVALID_EMAIL","Email không hợp lệ. Vui lòng dùng địa chỉ email không dấu.");
        return key;
    }
    public static void validatePassword(String password,String confirmation) {
        if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72 || password.isBlank()
            || password.chars().anyMatch(Character::isISOControl) || !password.chars().anyMatch(Character::isLetter)
            || !password.chars().anyMatch(Character::isDigit))
            throw new AuthException(400,"WEAK_PASSWORD","Mật khẩu cần ít nhất 12 ký tự, có chữ và số, tối đa 72 byte UTF-8.");
        if(!password.equals(confirmation)) throw new AuthException(400,"PASSWORD_MISMATCH","Mật khẩu xác nhận chưa khớp.");
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void register(AuthDtos.Register request) {
        String email=emailKey(request.email()); validatePassword(request.password(),request.confirmPassword());
        if(request.name()==null || request.name().strip().isBlank() || request.name().strip().length()>150)
            throw new AuthException(400,"INVALID_NAME","Vui lòng nhập họ và tên hợp lệ.");
        settings.requireDelivery();
        if(users.findByEmailKey(email).isPresent()) throw new AuthException(409,"EMAIL_EXISTS","Email đã được sử dụng. Hãy đăng nhập hoặc gửi lại mã xác thực.");
        User user=newCustomer(request.name(),email,passwords.encode(request.password()),false);
        users.saveAndFlush(user); issue(user,VERIFY);
    }
    public User newCustomer(String name,String email,String passwordHash,boolean verified) {
        User user=new User(); user.setName(name.strip()); user.setEmail(email); user.setPasswordHash(passwordHash);
        user.setRole(UserRole.CUSTOMER); user.setIsActive(true); user.setCreatedAt(now()); user.setUpdatedAt(now());
        if(verified) user.setEmailVerifiedAt(now()); return user;
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void resend(String email) {
        settings.requireDelivery();
        User user=users.lockByEmailKey(emailKey(email)).orElseThrow(()->new AuthException(400,"VERIFICATION_UNAVAILABLE","Không thể gửi mã cho tài khoản này."));
        if(user.getEmailVerifiedAt()!=null || !Boolean.TRUE.equals(user.getIsActive()))
            throw new AuthException(400,"VERIFICATION_UNAVAILABLE","Tài khoản đã xác thực hoặc không thể nhận mã.");
        issue(user,VERIFY);
    }
    // Caller holds the user row lock (or inserted this user in the same transaction).
    public void issue(User user,String purpose) {
        var challenge=challenges.findByUserIdAndPurpose(user.getId(),purpose).orElseGet(AuthChallenge::new);
        LocalDateTime current=now();
        if(challenge.getSentAt()!=null && challenge.getSentAt().plusSeconds(60).isAfter(current)) {
            int remaining=(int)Math.ceil(Duration.between(current,challenge.getSentAt().plusSeconds(60)).toMillis()/1000.0);
            throw new AuthException(429,"RESEND_COOLDOWN","Vui lòng chờ trước khi gửi lại mã.",remaining);
        }
        String code=mac.generate();
        challenge.setUserId(user.getId()); challenge.setPurpose(purpose); challenge.setNonce(UUID.randomUUID().toString());
        challenge.setCodeMac(mac.hash(challenge,code)); challenge.setExpiresAt(current.plusMinutes(10));
        challenge.setAttempts(0); challenge.setConsumedAt(null); challenge.setSentAt(null);
        challenges.save(challenge);
        try {mail.send(user.getEmail(),code,purpose); challenge.setSentAt(now());}
        catch(AuthException e) {challenge.setConsumedAt(now()); throw e;}
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void verify(String email,String code) {
        User user=users.lockByEmailKey(emailKey(email)).orElseThrow(()->invalidCode());
        if(!Boolean.TRUE.equals(user.getIsActive())) throw new AuthException(403,"ACCOUNT_LOCKED","Tài khoản đang bị khóa.");
        consume(user,VERIFY,code); user.setEmailVerifiedAt(now()); user.setUpdatedAt(now());
    }
    private void consume(User user,String purpose,String code) {
        var challenge=challenges.findByUserIdAndPurpose(user.getId(),purpose).orElseThrow(AuthService::invalidCode);
        if(challenge.getConsumedAt()!=null || challenge.getSentAt()==null) throw invalidCode();
        if(!challenge.getExpiresAt().isAfter(now())) throw new AuthException(400,"CODE_EXPIRED","Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
        if(challenge.getAttempts()>=5) throw new AuthException(429,"CODE_ATTEMPTS_EXCEEDED","Mã đã bị khóa sau 5 lần nhập sai. Vui lòng yêu cầu mã mới.",60);
        if(code==null || !code.matches("[0-9]{6}") || !mac.matches(challenge,code)) {
            challenge.setAttempts(challenge.getAttempts()+1);
            if(challenge.getAttempts()>=5) throw new AuthException(429,"CODE_ATTEMPTS_EXCEEDED","Mã đã bị khóa sau 5 lần nhập sai. Vui lòng yêu cầu mã mới.",60);
            throw invalidCode();
        }
        challenge.setConsumedAt(now());
    }
    private static AuthException invalidCode() {return new AuthException(400,"INVALID_CODE","Mã xác minh không hợp lệ hoặc đã được sử dụng.");}
    @Transactional(readOnly=true)
    public User authenticate(String email,String password) {
        if(password==null || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new AuthException(401,"INVALID_CREDENTIALS","Email hoặc mật khẩu không đúng.");
        User user=users.findByEmailKey(emailKey(email)).orElse(null);
        String hash=user==null || !user.getPasswordHash().startsWith("$2")?dummyHash:user.getPasswordHash();
        boolean valid=passwords.matches(password,hash);
        if(user==null || !valid || !user.getPasswordHash().startsWith("$2"))
            throw new AuthException(401,"INVALID_CREDENTIALS","Email hoặc mật khẩu không đúng.");
        requireActive(user); return user;
    }
    public void requireActive(User user) {
        if(!Boolean.TRUE.equals(user.getIsActive())) throw new AuthException(403,"ACCOUNT_LOCKED","Tài khoản đang bị khóa.");
        if(user.getEmailVerifiedAt()==null) throw new AuthException(403,"EMAIL_NOT_VERIFIED","Vui lòng xác thực email trước khi đăng nhập.");
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void forgot(String email) {
        settings.requireDelivery(); // Global configuration failure is independent of account existence.
        var user=users.lockByEmailKey(emailKey(email));
        if(user.isEmpty() || user.get().getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.get().getIsActive())) return;
        try {issue(user.get(),RESET);} catch(AuthException ignored) {
            // Cooldown/delivery errors have the exact same external response as an unknown address.
        }
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void reset(AuthDtos.Reset request) {
        validatePassword(request.password(),request.confirmPassword());
        User user=users.lockByEmailKey(emailKey(request.email())).orElseThrow(AuthService::invalidCode);
        if(user.getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.getIsActive())) throw invalidCode();
        consume(user,RESET,request.code());
        user.setPasswordHash(passwords.encode(request.password())); user.setAuthVersion(user.getAuthVersion()+1); user.setUpdatedAt(now());
    }
    @Transactional(readOnly=true)
    public User current(AuthPrincipal principal) {
        User user=users.findById(principal.id()).orElseThrow(()->new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
        if(user.getAuthVersion()!=principal.version() || user.getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.getIsActive()))
            throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        return user;
    }
}
