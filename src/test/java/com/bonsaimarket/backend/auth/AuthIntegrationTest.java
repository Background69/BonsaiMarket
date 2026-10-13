package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Real service, transactions, ORM and MockMvc against isolated H2. No live MySQL/SMTP/provider calls. */
@SpringBootTest(properties={
    "spring.profiles.active=auth-test", "spring.datasource.url=jdbc:h2:mem:auth;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
    "auth.otp-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", "spring.mail.host=mail.invalid", "auth.mail-from=test@example.com",
    "auth.google-client-id=fixture-google", "auth.google-client-secret=fixture-secret",
    "auth.facebook-client-id=fixture-facebook", "auth.facebook-client-secret=fixture-secret", "auth.facebook-api-version=v99.0"})
@AutoConfigureMockMvc @Import(AuthIntegrationTest.TestBeans.class)
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired OAuthAccounts oauth;
    @Autowired UserRepository users;
    @Autowired ChallengeRepository challenges;
    @Autowired IdentityRepository identities;
    @Autowired PasswordEncoder passwords;
    @Autowired OtpMac mac;
    @Autowired MutableClock clock;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean AuthMailSender mail;
    @MockitoBean(name="authMailExecutor") java.util.concurrent.Executor resetExecutor;
    private static final AtomicInteger sequence=new AtomicInteger();
    private final Map<String,String> codes=new ConcurrentHashMap<>();
    private static final String PASSWORD="ValidPassword123";
    @TestConfiguration static class TestBeans {
        @Bean @Primary MutableClock testClock() {return new MutableClock();}
        @Bean RoleProbe roleProbe() {return new RoleProbe();}
    }
    static class MutableClock extends Clock {
        private Instant instant=Instant.parse("2026-10-12T00:00:00Z");
        void advance(long seconds) {instant=instant.plusSeconds(seconds);}
        @Override public ZoneId getZone() {return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone) {return this;}
        @Override public Instant instant() {return instant;}
    }
    @RestController static class RoleProbe {
        @GetMapping("/api/admin/auth-probe") String admin() {return "admin";}
        @GetMapping("/api/seller/auth-probe") String seller() {return "seller";}
    }
    @BeforeEach void captureMail() {
        doAnswer(invocation->{((Runnable)invocation.getArgument(0)).run();return null;}).when(resetExecutor).execute(any());
        doAnswer(invocation->{codes.put(invocation.getArgument(0)+":"+invocation.getArgument(2),invocation.getArgument(1));return null;}).when(mail).send(anyString(),anyString(),anyString());
    }
    String email() {return "auth-"+sequence.incrementAndGet()+"@example.com";}
    void register(String email) {auth.register(new AuthDtos.Register("Người dùng kiểm thử",email,PASSWORD,PASSWORD));}
    String code(String email,String purpose) {return codes.get(email+":"+purpose);}
    void verified(String email) {register(email);auth.verify(email,code(email,AuthService.VERIFY));}
    AuthException failure(Runnable runnable,String code) {var e=assertThrows(AuthException.class,runnable::run);assertEquals(code,e.code);return e;}
    MockHttpSession login(String email) throws Exception {
        var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
            .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    void edit(String email,java.util.function.Consumer<User> change) {
        new TransactionTemplate(transactions).executeWithoutResult(status->change.accept(users.lockByEmailKey(email).orElseThrow()));
    }
    @Test void registrationPersistsBuyerHashAndHmacWithoutVerification() {
        String email=email();register(email);var user=users.findByEmailKey(email).orElseThrow();
        assertEquals(UserRole.CUSTOMER,user.getRole());assertNull(user.getEmailVerifiedAt());assertTrue(passwords.matches(PASSWORD,user.getPasswordHash()));
        var challenge=challenges.findByUserIdAndPurpose(user.getId(),AuthService.VERIFY).orElseThrow();
        assertTrue(code(email,AuthService.VERIFY).matches("[0-9]{6}"));assertEquals(64,challenge.getCodeMac().length());
        assertNotEquals(code(email,AuthService.VERIFY),challenge.getCodeMac());assertEquals(auth.now().plusMinutes(10),challenge.getExpiresAt());
    }
    @Test void emailCanonicalizationAndDuplicateAreConsistent() {
        String email=email();register("  "+email.toUpperCase(Locale.ROOT)+"  ");failure(()->register(email),"EMAIL_EXISTS");
    }
    @Test void invalidEmailPasswordConfirmationAndBlankNameRejected() {
        failure(()->register("invalid"),"INVALID_EMAIL");
        for(String password:List.of("short1"," ".repeat(12),"a".repeat(73),"à".repeat(36)+"1","abcdefghijkl"))
            failure(()->auth.register(new AuthDtos.Register("Test",email(),password,password)),"WEAK_PASSWORD");
        failure(()->auth.register(new AuthDtos.Register("Test",email(),PASSWORD,"different")),"PASSWORD_MISMATCH");
        failure(()->auth.register(new AuthDtos.Register("  ",email(),PASSWORD,PASSWORD)),"INVALID_NAME");
    }
    @Test void verificationActivatesOnceAndDoesNotAutoLogin() {
        String email=email();register(email);String code=code(email,AuthService.VERIFY);auth.verify(email,code);
        assertNotNull(users.findByEmailKey(email).orElseThrow().getEmailVerifiedAt());failure(()->auth.verify(email,code),"INVALID_CODE");
    }
    @Test void wrongExpiredAndLockedOtpNeverActivateAccount() {
        String email=email();register(email);String real=code(email,AuthService.VERIFY),wrong=real.equals("000000")?"111111":"000000";
        for(int i=0;i<4;i++) failure(()->auth.verify(email,wrong),"INVALID_CODE");
        failure(()->auth.verify(email,wrong),"CODE_ATTEMPTS_EXCEEDED");failure(()->auth.verify(email,real),"CODE_ATTEMPTS_EXCEEDED");
        assertNull(users.findByEmailKey(email).orElseThrow().getEmailVerifiedAt());
        String expired=email();register(expired);clock.advance(601);failure(()->auth.verify(expired,code(expired,AuthService.VERIFY)),"CODE_EXPIRED");
    }
    @Test void resendCooldownReplacesOldCodeAndMac() {
        String email=email();register(email);String old=code(email,AuthService.VERIFY);
        failure(()->auth.resend(email),"RESEND_COOLDOWN");clock.advance(61);auth.resend(email);
        var user=users.findByEmailKey(email).orElseThrow();var c=challenges.findByUserIdAndPurpose(user.getId(),AuthService.VERIFY).orElseThrow();
        assertTrue(mac.matches(c,code(email,AuthService.VERIFY)));
        // Random codes may coincide; the nonce still rotates and only the current MAC is stored.
        if(!old.equals(code(email,AuthService.VERIFY))) failure(()->auth.verify(email,old),"INVALID_CODE");
    }
    @Test void mailFailurePreservesPendingAccountAndAllowsRetry() {
        String email=email();doThrow(new AuthException(503,"MAIL_DELIVERY_FAILED","failure")).when(mail).send(eq(email),anyString(),anyString());
        failure(()->register(email),"MAIL_DELIVERY_FAILED");assertTrue(users.findByEmailKey(email).isPresent());
        var user=users.findByEmailKey(email).orElseThrow();assertNull(user.getEmailVerifiedAt());
        assertNotNull(challenges.findByUserIdAndPurpose(user.getId(),AuthService.VERIFY).orElseThrow().getConsumedAt());
        captureMail();auth.resend(email);auth.verify(email,code(email,AuthService.VERIFY));
    }
    @Test void unverifiedLockedAndWrongPasswordLoginFail() {
        String email=email();register(email);failure(()->auth.authenticate(email,PASSWORD),"EMAIL_NOT_VERIFIED");
        failure(()->auth.authenticate(email,"wrong"),"INVALID_CREDENTIALS");
        auth.verify(email,code(email,AuthService.VERIFY));edit(email,user->user.setIsActive(false));failure(()->auth.authenticate(email,PASSWORD),"ACCOUNT_LOCKED");
        failure(()->auth.authenticate(email(),PASSWORD),"INVALID_CREDENTIALS");
        failure(()->auth.authenticate(email,"à".repeat(72)),"INVALID_CREDENTIALS");
    }
    @Test void sessionLoginMeCsrfRotationAndLogout() throws Exception {
        String email=email();verified(email);
        var initial=new MockHttpSession();String before=initial.getId();
        var result=mvc.perform(post("/api/auth/login").session(initial).with(csrf()).contentType("application/json")
            .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("customer")).andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);assertNotEquals(before,session.getId());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());assertTrue(session.isInvalid());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void actualCsrfEndpointTokenWorksAndIsRotatedOnLogin() throws Exception {
        String email=email();verified(email);var session=new MockHttpSession();
        var token=(org.springframework.security.web.csrf.CsrfToken)mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn().getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        mvc.perform(post("/api/auth/login").session(session).header(token.getHeaderName(),token.getToken()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).header(token.getHeaderName(),token.getToken())).andExpect(status().isForbidden());
    }
    @Test void forgotResponseDoesNotEnumerateAndResetRevokesOldSessions() throws Exception {
        String email=email();verified(email);var session=login(email);
        var known=mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\"}")).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        var unknown=mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\""+email()+"\"}")).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();assertEquals(known,unknown);
        String reset=code(email,AuthService.RESET);assertNotNull(reset);
        if(!reset.equals(code(email,AuthService.VERIFY))) failure(()->auth.reset(new AuthDtos.Reset(email,code(email,AuthService.VERIFY),PASSWORD,PASSWORD)),"INVALID_CODE");
        auth.reset(new AuthDtos.Reset(email,reset,"UpdatedPassword456","UpdatedPassword456"));
        failure(()->auth.authenticate(email,PASSWORD),"INVALID_CREDENTIALS");assertNotNull(auth.authenticate(email,"UpdatedPassword456"));
        failure(()->auth.reset(new AuthDtos.Reset(email,reset,PASSWORD,PASSWORD)),"INVALID_CODE");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void invalidAndExpiredResetNeverChangePassword() {
        String email=email();verified(email);auth.forgot(email);String reset=code(email,AuthService.RESET);
        String wrong=reset.equals("000000")?"111111":"000000";
        failure(()->auth.reset(new AuthDtos.Reset(email,wrong,PASSWORD,PASSWORD)),"INVALID_CODE");
        clock.advance(601);failure(()->auth.reset(new AuthDtos.Reset(email,reset,PASSWORD,PASSWORD)),"CODE_EXPIRED");assertNotNull(auth.authenticate(email,PASSWORD));
    }
    @Test void roleAuthorizationUsesDatabaseAndUpdatesOnNextRequest() throws Exception {
        String email=email();verified(email);var session=login(email);
        mvc.perform(get("/api/admin/auth-probe").session(session)).andExpect(status().isForbidden());mvc.perform(get("/api/seller/auth-probe").session(session)).andExpect(status().isForbidden());
        edit(email,user->user.setRole(UserRole.SELLER));mvc.perform(get("/api/seller/auth-probe").session(session)).andExpect(status().isOk());mvc.perform(get("/api/admin/auth-probe").session(session)).andExpect(status().isForbidden());
        edit(email,user->user.setRole(UserRole.ADMIN));mvc.perform(get("/api/admin/auth-probe").session(session)).andExpect(status().isOk());
        edit(email,user->user.setIsActive(false));mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    OAuthAccounts.External external(String provider,String email,boolean verified) {return new OAuthAccounts.External(provider,"subject-"+sequence.incrementAndGet(),"Provider user",email,verified,clock.millis()+600_000);}
    @Test void oauthMapsBySubjectCreatesCustomerAndNeverMergesByEmail() {
        String email=email();var external=external("google",email,true);User user=oauth.resolve(external,null);assertEquals(UserRole.CUSTOMER,user.getRole());assertNotNull(user.getEmailVerifiedAt());
        assertEquals(user.getId(),oauth.resolve(external,null).getId());failure(()->oauth.resolve(external("google",email,true),null),"OAUTH_EMAIL_COLLISION");
        String existing=email();verified(existing);failure(()->oauth.resolve(external("google",existing,true),null),"OAUTH_EMAIL_COLLISION");
    }
    @Test void facebookMissingOrUntrustedEmailNeedsOwnVerification() {
        var external=external("facebook",null,false);assertNull(oauth.resolve(external,null));
        String email=email();oauth.complete(external,new AuthDtos.Complete("Facebook user",email));
        failure(()->oauth.resolve(external,null),"EMAIL_NOT_VERIFIED");auth.verify(email,code(email,AuthService.VERIFY));assertNotNull(oauth.resolve(external,null));
        assertNull(oauth.resolve(external("facebook",email(),false),null));
    }
    @Test void oauthLinkRequiresUnexpiredReauthenticatedIntentAndUniqueSubject() {
        String email=email();verified(email);var user=users.findByEmailKey(email).orElseThrow();var external=external("google",email,true);
        failure(()->oauth.resolve(external,new OAuthAccounts.LinkIntent(user.getId(),user.getAuthVersion(),"google",clock.millis()-1)),"OAUTH_LINK_EXPIRED");
        assertEquals(user.getId(),oauth.resolve(external,new OAuthAccounts.LinkIntent(user.getId(),user.getAuthVersion(),"google",clock.millis()+1000)).getId());
        String other=email();verified(other);var second=users.findByEmailKey(other).orElseThrow();
        failure(()->oauth.resolve(external,new OAuthAccounts.LinkIntent(second.getId(),second.getAuthVersion(),"google",clock.millis()+1000)),"OAUTH_ALREADY_LINKED");
    }
    @Test void oauthRequestUsesStateNoncePkceAndInvalidCallbackFailsClosed() throws Exception {
        var url=mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertNotNull(url);for(String parameter:List.of("state=","nonce=","code_challenge=","code_challenge_method=S256")) assertTrue(url.contains(parameter));
        mvc.perform(get("/login/oauth2/code/google").param("code","untrusted").param("state","bad-state"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost:5173/login?authError=OAUTH_FAILED"));
        var session=new MockHttpSession();mvc.perform(get("/oauth2/authorization/google").session(session));
        mvc.perform(get("/login/oauth2/code/google").session(session).param("code","untrusted").param("state","wrong"))
            .andExpect(redirectedUrl("http://localhost:5173/login?authError=OAUTH_FAILED"));assertTrue(session.isInvalid());
    }
    @Test void loginRateLimitAndSpoofedHeadersCannotBypassIpBucket() throws Exception {
        String email=email();verified(email);
        for(int i=0;i<10;i++) mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\"wrong\"}").header("X-Forwarded-For","spoof-"+i)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","900"));
    }
    @Test void csrfCorsDtoRoleInjectionAndPublicCatalogBoundaries() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Test\",\"email\":\"bad\",\"password\":\"short\",\"confirmPassword\":\"short\"}")).andExpect(status().isBadRequest());
        String email=email();mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Test\",\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\",\"confirmPassword\":\""+PASSWORD+"\",\"role\":\"ADMIN\"}")).andExpect(status().isCreated());assertEquals(UserRole.CUSTOMER,users.findByEmailKey(email).orElseThrow().getRole());
        mvc.perform(options("/api/auth/login").header("Origin","https://evil.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
        mvc.perform(options("/api/auth/login").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
        for(String path:List.of("/api/products","/api/categories","/api/stores")) mvc.perform(get(path)).andExpect(status().isOk());
    }
    @Test void concurrentVerificationConsumesChallengeExactlyOnce() throws Exception {
        String email=email();register(email);String code=code(email,AuthService.VERIFY);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();for(int i=0;i<2;i++) results.add(pool.submit(()->{start.await();try {auth.verify(email,code);return true;} catch(AuthException e) {return false;}}));
            start.countDown();int successes=0;for(var result:results) if(result.get(10,TimeUnit.SECONDS)) successes++;assertEquals(1,successes);
        } finally {pool.shutdownNow();}
    }
    @Test void oversizedAuthBodiesAreRejectedBeforeMailOrPasswordWork() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("x".repeat(8193)))
            .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("REQUEST_TOO_LARGE"));
        verifyNoInteractions(mail);
    }
}
