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
    public static final int OTP_TTL_SECONDS=600, MAX_CODE_ATTEMPTS=5, RESEND_SECONDS=60;
    public static class PendingDeliveryFailure extends AuthException {
        final User pending;
        public PendingDeliveryFailure(User pending,AuthException cause) {super(cause.status,cause.code,cause.getMessage(),cause.retryAfter); this.pending=pending;}
    }
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
        // Chuẩn hóa email ASCII để tránh trùng do collation bỏ dấu của MySQL.
        if(key.length()>255 || !key.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}"))
            throw new AuthException(400,"INVALID_EMAIL","Email không hợp lệ. Vui lòng dùng địa chỉ email không dấu.");
        return key;
    }
    public static void validatePassword(String password,String confirmation) {
        if(password!=null && password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new AuthException(400,"WEAK_PASSWORD","Mật khẩu quá dài, vui lòng rút ngắn lại.");
        if(password==null || password.length()<12 || password.isBlank()
            || password.chars().anyMatch(Character::isISOControl) || !password.chars().anyMatch(Character::isLetter)
            || !password.chars().anyMatch(Character::isDigit))
            throw new AuthException(400,"WEAK_PASSWORD","Tối thiểu 12 ký tự, bao gồm cả chữ và số.");
        if(!password.equals(confirmation)) throw new AuthException(400,"PASSWORD_MISMATCH","Mật khẩu xác nhận chưa khớp.");
    }
    @Transactional(noRollbackFor=AuthException.class)
    public User register(AuthDtos.Register request) {
        String email=emailKey(request.email()); validatePassword(request.password(),request.confirmPassword());
        if(request.name()==null || request.name().strip().isBlank() || request.name().strip().length()>150)
            throw new AuthException(400,"INVALID_NAME","Vui lòng nhập họ và tên hợp lệ.");
        settings.requireDelivery();
        if(users.findByEmailKey(email).isPresent()) throw new AuthException(409,"EMAIL_EXISTS","Email đã được sử dụng. Hãy đăng nhập hoặc gửi lại mã xác thực.");
        User user=newCustomer(request.name(),email,passwords.encode(request.password()),false);
        users.saveAndFlush(user);
        try {issue(user,VERIFY);} catch(AuthException e) {if("MAIL_DELIVERY_FAILED".equals(e.code)) throw new PendingDeliveryFailure(user,e); throw e;}
        return user;
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
    // Bên gọi phải giữ khóa dòng user hoặc vừa tạo user trong cùng giao dịch.
    public void issue(User user,String purpose) {
        var challenge=challenges.findByUserIdAndPurpose(user.getId(),purpose).orElseGet(AuthChallenge::new);
        LocalDateTime current=now();
        if(challenge.getSentAt()!=null && challenge.getSentAt().plusSeconds(RESEND_SECONDS).isAfter(current)) {
            int remaining=(int)Math.ceil(Duration.between(current,challenge.getSentAt().plusSeconds(RESEND_SECONDS)).toMillis()/1000.0);
            throw new AuthException(429,"RESEND_COOLDOWN","Vui lòng chờ trước khi gửi lại mã.",remaining);
        }
        String code=mac.generate();
        challenge.setUserId(user.getId()); challenge.setPurpose(purpose); challenge.setNonce(UUID.randomUUID().toString());
        challenge.setCodeMac(mac.hash(challenge,code)); challenge.setExpiresAt(current.plusSeconds(OTP_TTL_SECONDS));
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
    private AuthChallenge consume(User user,String purpose,String code) {
        var challenge=challenges.lockByUserIdAndPurpose(user.getId(),purpose).orElseThrow(AuthService::invalidCode);
        return consume(challenge,code,mac.matches(challenge,code==null?"":code));
    }
    private AuthChallenge consume(AuthChallenge challenge,String code,boolean matched) {
        if(challenge.getConsumedAt()!=null || challenge.getSentAt()==null) throw invalidCode();
        if(!challenge.getExpiresAt().isAfter(now())) throw new AuthException(400,"CODE_EXPIRED","Mã đã hết hạn. Vui lòng yêu cầu mã mới.");
        if(challenge.getAttempts()>=MAX_CODE_ATTEMPTS) throw new AuthException(429,"CODE_ATTEMPTS_EXCEEDED","Mã đã bị khóa sau 5 lần nhập sai. Vui lòng yêu cầu mã mới.",RESEND_SECONDS);
        if(code==null || !code.matches("[0-9]{6}") || !matched) {
            challenge.setAttempts(challenge.getAttempts()+1);
            if(challenge.getAttempts()>=MAX_CODE_ATTEMPTS) throw new AuthException(429,"CODE_ATTEMPTS_EXCEEDED","Mã đã bị khóa sau 5 lần nhập sai. Vui lòng yêu cầu mã mới.",RESEND_SECONDS);
            throw invalidCode();
        }
        challenge.setConsumedAt(now());
        return challenge;
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
        settings.requireDelivery(); // Lỗi cấu hình toàn hệ thống không phụ thuộc tài khoản có tồn tại hay không.
        var user=users.lockByEmailKey(emailKey(email));
        if(user.isEmpty() || user.get().getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.get().getIsActive())) return;
        try {issue(user.get(),RESET);} catch(AuthException ignored) {
            // Lỗi chờ gửi lại/gửi email trả phản hồi như địa chỉ chưa tồn tại.
        }
    }
    @Transactional(noRollbackFor=AuthException.class)
    public AuthFlowState.ResetPermit verifyResetCode(String email,String code) {
        User user=users.lockByEmailKey(emailKey(email)).orElse(null);
        // Mỗi nhánh đều thực hiện hai lần tra cứu và một lần so sánh HMAC.
        // Giá trị thay thế không được lưu hoặc chấp nhận; không kết thúc sớm theo sự tồn tại của tài khoản.
        AuthChallenge placeholder=new AuthChallenge();
        placeholder.setUserId(0L); placeholder.setPurpose(RESET); placeholder.setNonce("00000000-0000-0000-0000-000000000000");
        placeholder.setCodeMac("0".repeat(64));
        AuthChallenge challenge=challenges.lockByUserIdAndPurpose(user==null?0L:user.getId(),RESET).orElse(placeholder);
        boolean matched=mac.matches(challenge,code==null?"":code);
        if(user==null || user.getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.getIsActive()) || challenge==placeholder) throw invalidCode();
        consume(challenge,code,matched);
        return new AuthFlowState.ResetPermit(user.getId(),user.getAuthVersion(),challenge.getNonce(),clock.millis()+AuthFlowState.PERMIT_SECONDS*1000);
    }
    @Transactional
    public void reset(AuthFlowState.ResetPermit permit,AuthDtos.Reset request) {
        if(permit==null || permit.expiresAt()<=clock.millis()) throw AuthFlowState.permitExpired();
        User user=users.lockById(permit.userId()).orElseThrow(AuthFlowState::permitExpired);
        if(user.getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.getIsActive()) || user.getAuthVersion()!=permit.version()) throw AuthFlowState.permitExpired();
        var challenge=challenges.lockByUserIdAndPurpose(user.getId(),RESET).orElseThrow(AuthFlowState::permitExpired);
        if(challenge.getConsumedAt()==null || !challenge.getNonce().equals(permit.nonce()) || permit.expiresAt()<=clock.millis()) throw AuthFlowState.permitExpired();
        validatePassword(request.password(),request.confirmPassword());
        user.setPasswordHash(passwords.encode(request.password())); user.setAuthVersion(user.getAuthVersion()+1); user.setUpdatedAt(now());
    }
    /** Đổi mật khẩu cần xác minh mật khẩu hiện tại và tăng auth_version để vô hiệu các phiên khác. */
    @Transactional(noRollbackFor=AuthException.class)
    public User changePassword(AuthPrincipal principal,AuthDtos.ChangePassword request) {
        User user=users.lockById(principal.id()).orElseThrow(()->new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
        if(user.getAuthVersion()!=principal.version()) throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        requireActive(user);
        String hash=user.getPasswordHash();
        if(hash==null || !hash.startsWith("$2"))
            throw new AuthException(400,"PASSWORD_NOT_SET","Tài khoản này chưa có mật khẩu. Hãy dùng chức năng Quên mật khẩu để tạo mật khẩu.");
        String current=request.currentPassword();
        // Trả 400 vì SPA coi 401 là hết phiên và sẽ đăng xuất người dùng.
        if(current==null || current.getBytes(StandardCharsets.UTF_8).length>72 || !passwords.matches(current,hash))
            throw new AuthException(400,"INVALID_CURRENT_PASSWORD","Mật khẩu hiện tại không đúng.");
        validatePassword(request.newPassword(),request.confirmPassword());
        if(passwords.matches(request.newPassword(),hash))
            throw new AuthException(400,"PASSWORD_UNCHANGED","Mật khẩu mới phải khác mật khẩu hiện tại.");
        user.setPasswordHash(passwords.encode(request.newPassword())); user.setAuthVersion(user.getAuthVersion()+1); user.setUpdatedAt(now());
        return user;
    }
    @Transactional(readOnly=true)
    public User pendingAfterPasswordProof(String email,String password) {
        User user=users.findByEmailKey(emailKey(email)).orElseThrow(AuthFlowState::verificationExpired);
        if(user.getEmailVerifiedAt()!=null || !Boolean.TRUE.equals(user.getIsActive()) || user.getRole()!=UserRole.CUSTOMER
            || !passwords.matches(password,user.getPasswordHash())) throw AuthFlowState.verificationExpired();
        return user;
    }
    private void requirePending(User user,AuthFlowState.Verification context) {
        if(context.expiresAt()<=clock.millis() || !user.getEmail().equals(context.email()) || user.getAuthVersion()!=context.version()
            || user.getRole()!=UserRole.CUSTOMER || user.getEmailVerifiedAt()!=null || !Boolean.TRUE.equals(user.getIsActive())) throw AuthFlowState.verificationExpired();
    }
    @Transactional(readOnly=true)
    public AuthDtos.VerificationContext verificationContext(AuthFlowState.Verification context) {
        User user=users.findById(context.userId()).orElseThrow(AuthFlowState::verificationExpired); requirePending(user,context);
        var challenge=challenges.findByUserIdAndPurpose(user.getId(),VERIFY).orElseThrow(AuthFlowState::verificationExpired);
        int remaining=challenge.getSentAt()==null?0:(int)Math.max(0,Math.ceil(Duration.between(now(),challenge.getSentAt().plusSeconds(RESEND_SECONDS)).toMillis()/1000.0));
        int at=user.getEmail().indexOf('@');
        return new AuthDtos.VerificationContext(user.getEmail().substring(0,Math.min(2,at))+"•••"+user.getEmail().substring(at),remaining,context.contextId());
    }
    @Transactional(noRollbackFor=AuthException.class)
    public User verifyPending(AuthFlowState.Verification context,String code) {
        User user=users.lockById(context.userId()).orElseThrow(AuthFlowState::verificationExpired); requirePending(user,context);
        consume(user,VERIFY,code); user.setEmailVerifiedAt(now()); user.setUpdatedAt(now()); return user;
    }
    @Transactional(noRollbackFor=AuthException.class)
    public void resendPending(AuthFlowState.Verification context) {
        User user=users.lockById(context.userId()).orElseThrow(AuthFlowState::verificationExpired); requirePending(user,context);
        settings.requireDelivery(); issue(user,VERIFY);
    }
    @Transactional(readOnly=true)
    public User current(AuthPrincipal principal) {
        User user=users.findById(principal.id()).orElseThrow(()->new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."));
        if(user.getAuthVersion()!=principal.version() || user.getEmailVerifiedAt()==null || !Boolean.TRUE.equals(user.getIsActive()))
            throw new AuthException(401,"SESSION_EXPIRED","Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        return user;
    }
}
