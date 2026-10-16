package com.bonsaimarket.backend.auth;

import com.bonsaimarket.backend.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
    @Autowired OAuthHandlers oauthHandlers;
    @Autowired UserRepository users;
    @Autowired ChallengeRepository challenges;
    @Autowired IdentityRepository identities;
    @Autowired PasswordEncoder passwords;
    @MockitoSpyBean OtpMac mac;
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
        if(!reset.equals(code(email,AuthService.VERIFY))) failure(()->auth.verifyResetCode(email,code(email,AuthService.VERIFY)),"INVALID_CODE");
        var permit=auth.verifyResetCode(email,reset);
        auth.reset(permit,new AuthDtos.Reset("UpdatedPassword456","UpdatedPassword456"));
        failure(()->auth.authenticate(email,PASSWORD),"INVALID_CREDENTIALS");assertNotNull(auth.authenticate(email,"UpdatedPassword456"));
        failure(()->auth.reset(permit,new AuthDtos.Reset(PASSWORD,PASSWORD)),"RESET_AUTHORIZATION_EXPIRED");
        failure(()->auth.verifyResetCode(email,reset),"INVALID_CODE");
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void invalidAndExpiredResetNeverChangePassword() {
        String email=email();verified(email);auth.forgot(email);String reset=code(email,AuthService.RESET);
        String wrong=reset.equals("000000")?"111111":"000000";
        failure(()->auth.verifyResetCode(email,wrong),"INVALID_CODE");
        clock.advance(601);failure(()->auth.verifyResetCode(email,reset),"CODE_EXPIRED");assertNotNull(auth.authenticate(email,PASSWORD));
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

    // All endpoint tests below use the real services/session/CSRF and isolated H2 + mock mail.
    ResultActions flowPost(String path,MockHttpSession session,Map<String,String> body) throws Exception {
        body=new HashMap<>(body);
        if(path.equals("forgot-password"))body.putIfAbsent("flowId",UUID.randomUUID().toString());
        if(session!=null && !session.isInvalid()) {
            if(List.of("verify-reset-code","reset-password","resend-reset-code").contains(path) && session.getAttribute(AuthFlowState.RESET) instanceof AuthFlowState.ResetFlow flow)body.putIfAbsent("flowId",flow.flowId());
            if(List.of("verify-email","resend-verification").contains(path) && session.getAttribute(AuthFlowState.VERIFICATION) instanceof AuthFlowState.Verification context)body.putIfAbsent("contextId",context.contextId());
        }
        var request=post("/api/auth/"+path).with(csrf()).with(r->{r.setRemoteAddr("198.51.100."+sequence.incrementAndGet());return r;})
            .contentType("application/json").content(new tools.jackson.databind.ObjectMapper().writeValueAsString(body));
        if(session!=null) request.session(session);
        return mvc.perform(request);
    }
    MockHttpSession registerFlow(String email) throws Exception {
        return (MockHttpSession)flowPost("register",null,Map.of("name","Flow user","email",email,"password",PASSWORD,"confirmPassword",PASSWORD))
            .andExpect(status().isCreated()).andReturn().getRequest().getSession(false);
    }
    MockHttpSession resetFlow(String email) throws Exception {
        return (MockHttpSession)flowPost("forgot-password",null,Map.of("email",email))
            .andExpect(status().isAccepted()).andReturn().getRequest().getSession(false);
    }
    MockHttpSession authorizedReset(String email) throws Exception {
        var session=resetFlow(email); flowPost("verify-reset-code",session,Map.of("code",code(email,AuthService.RESET)))
            .andExpect(status().isOk()); return session;
    }
    void resetFinish(MockHttpSession session,String password,String confirmation,int expected) throws Exception {
        flowPost("reset-password",session,Map.of("password",password,"confirmPassword",confirmation)).andExpect(status().is(expected));
    }
    @Test void forgotKnownUnknownPendingLockedAndOauthAccountsHaveIdenticalPublicResponses() throws Exception {
        String known=email(),pending=email(),locked=email(),oauthEmail=email(),unknown=email();
        verified(known);register(pending);verified(locked);edit(locked,u->u.setIsActive(false));
        oauth.resolve(external("google",oauthEmail,true),null);
        String response=null;
        for(String address:List.of(known,unknown,pending,locked,oauthEmail)) {
            var result=flowPost("forgot-password",null,Map.of("email",address)).andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.exists").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist()).andReturn();
            String actual=result.getResponse().getContentAsString();if(response==null)response=actual;else assertEquals(response,actual);
        }
        assertNull(code(pending,AuthService.RESET));assertNull(code(locked,AuthService.RESET));assertNull(code(unknown,AuthService.RESET));
    }
    @Test void resetWithoutSessionFlowOrPermitAndClientForgedFieldsIsRejected() throws Exception {
        String email=email();verified(email);
        resetFinish(null,"UpdatedPassword456","UpdatedPassword456",409);
        var session=resetFlow(email);
        flowPost("reset-password",session,Map.of("password","UpdatedPassword456","confirmPassword","UpdatedPassword456","email",email,"code",code(email,AuthService.RESET),"verified","true"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESET_AUTHORIZATION_EXPIRED"));
        assertNotNull(auth.authenticate(email,PASSWORD));
        flowPost("verify-reset-code",new MockHttpSession(),Map.of("code",code(email,AuthService.RESET))).andExpect(status().isConflict());
    }
    @Test void invalidExpiredExhaustedUnknownAndIneligibleResetCodesDoNotGrantAndStayUniform() throws Exception {
        String known=email(),pending=email(),unknown=email();verified(known);register(pending);
        var session=resetFlow(known);String correct=code(known,AuthService.RESET),wrong=correct.equals("000000")?"111111":"000000";
        String generic=flowPost("verify-reset-code",session,Map.of("code",wrong)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        for(int i=1;i<5;i++) assertEquals(generic,flowPost("verify-reset-code",session,Map.of("code",wrong)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        assertEquals(generic,flowPost("verify-reset-code",session,Map.of("code",correct)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        assertEquals(5,challenges.findByUserIdAndPurpose(users.findByEmailKey(known).orElseThrow().getId(),AuthService.RESET).orElseThrow().getAttempts());
        for(String address:List.of(unknown,pending)) {
            var other=resetFlow(address);
            assertEquals(generic,flowPost("verify-reset-code",other,Map.of("code",wrong)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        }
        clock.advance(601);
        assertEquals(generic,flowPost("verify-reset-code",session,Map.of("code",correct)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        resetFinish(session,PASSWORD,PASSWORD,409);assertNotNull(auth.authenticate(known,PASSWORD));
    }
    @Test void resetRejectsWrongPurposeAndWrongAccountCode() throws Exception {
        String first=email(),second=email();verified(first);verified(second);
        var session=resetFlow(first);resetFlow(second);
        var challenge=challenges.findByUserIdAndPurpose(users.findByEmailKey(first).orElseThrow().getId(),AuthService.RESET).orElseThrow();
        // Remove the 1-in-a-million collision from a security test; production still uses random codes.
        new TransactionTemplate(transactions).executeWithoutResult(s->{var locked=users.lockByEmailKey(first).orElseThrow();var c=challenges.findByUserIdAndPurpose(locked.getId(),AuthService.RESET).orElseThrow();c.setCodeMac(mac.hash(c,"314159"));});
        for(String wrongPurpose:List.of(code(first,AuthService.VERIFY),code(second,AuthService.RESET))) {
            if(wrongPurpose.equals("314159"))wrongPurpose="271828";
            flowPost("verify-reset-code",session,Map.of("code",wrongPurpose,"email",second)).andExpect(status().isBadRequest());
        }
        assertNull(challenge.getConsumedAt());assertNotNull(auth.authenticate(first,PASSWORD));
    }
    @Test void resetPermitIsServerOnlyExpiresAfterFiveMinutesAndDoesNotAuthenticate() throws Exception {
        String email=email();verified(email);var session=authorizedReset(email);
        assertNotNull(challenges.findByUserIdAndPurpose(users.findByEmailKey(email).orElseThrow().getId(),AuthService.RESET).orElseThrow().getConsumedAt());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        clock.advance(300);resetFinish(session,"UpdatedPassword456","UpdatedPassword456",409);assertNotNull(auth.authenticate(email,PASSWORD));
    }
    @Test void resetPermitCannotCrossSessionsOrAccountsOrSurviveLogout() throws Exception {
        String first=email(),second=email();verified(first);verified(second);var session=authorizedReset(first);
        resetFinish(new MockHttpSession(),PASSWORD,PASSWORD,409);
        flowPost("reset-password",session,Map.of("password","UpdatedPassword456","confirmPassword","UpdatedPassword456","email",second,"userId",users.findByEmailKey(second).orElseThrow().getId().toString()))
            .andExpect(status().isOk());
        assertNotNull(auth.authenticate(first,"UpdatedPassword456"));assertNotNull(auth.authenticate(second,PASSWORD));
        var other=authorizedReset(second);flowPost("logout",other,Map.of()).andExpect(status().isOk());assertTrue(other.isInvalid());
        resetFinish(null,PASSWORD,PASSWORD,409);
    }
    @Test void resetValidationKeepsPermitUntilValidPasswordAndRevokesEveryOldSession() throws Exception {
        String email=email();verified(email);var old1=login(email);var old2=login(email);var session=authorizedReset(email);
        resetFinish(session,"UpdatedPassword456","MismatchPassword789",400);
        resetFinish(session,"a".repeat(12),"a".repeat(12),400);
        resetFinish(session,"đ".repeat(36)+"1","đ".repeat(36)+"1",400);
        resetFinish(session,"UpdatedPassword456","UpdatedPassword456",200);assertTrue(session.isInvalid());
        var user=users.findByEmailKey(email).orElseThrow();assertTrue(user.getPasswordHash().startsWith("$2"));assertTrue(passwords.matches("UpdatedPassword456",user.getPasswordHash()));assertEquals(1,user.getAuthVersion());
        mvc.perform(get("/api/auth/me").session(old1)).andExpect(status().isUnauthorized());mvc.perform(get("/api/auth/me").session(old2)).andExpect(status().isUnauthorized());
        resetFinish(null,"AnotherPassword789","AnotherPassword789",409);failure(()->auth.verifyResetCode(email,code(email,AuthService.RESET)),"INVALID_CODE");
    }
    @Test void resetResendCooldownAndRateLimitAreSameForKnownAndUnknown() throws Exception {
        String known=email(),unknown=email();verified(known);var a=resetFlow(known);var b=resetFlow(unknown);
        String first=flowPost("resend-reset-code",a,Map.of()).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60")).andReturn().getResponse().getContentAsString();
        assertEquals(first,flowPost("resend-reset-code",b,Map.of()).andExpect(status().isTooManyRequests()).andReturn().getResponse().getContentAsString());
        clock.advance(60);String sent=flowPost("resend-reset-code",a,Map.of()).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertEquals(sent,flowPost("resend-reset-code",b,Map.of()).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString());
        for(int i=0;i<2;i++){clock.advance(60);flowPost("resend-reset-code",a,Map.of()).andExpect(status().isAccepted());flowPost("resend-reset-code",b,Map.of()).andExpect(status().isAccepted());}
        clock.advance(60);flowPost("resend-reset-code",a,Map.of()).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        flowPost("resend-reset-code",b,Map.of()).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
    @Test void aNewResetRequestOrNewChallengeRevokesThePreviousPermit() throws Exception {
        String email=email();verified(email);var session=authorizedReset(email);
        clock.advance(60);flowPost("forgot-password",session,Map.of("email",email)).andExpect(status().isAccepted());resetFinish(session,PASSWORD,PASSWORD,409);
        clock.advance(60);var second=authorizedReset(email);clock.advance(60);auth.forgot(email);resetFinish(second,PASSWORD,PASSWORD,409);
    }
    @Test void concurrentResetPermitUseChangesPasswordAtMostOnce() throws Exception {
        String email=email();verified(email);auth.forgot(email);var permit=auth.verifyResetCode(email,code(email,AuthService.RESET));
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();for(int i=0;i<2;i++)results.add(pool.submit(()->{start.await();try{auth.reset(permit,new AuthDtos.Reset("UpdatedPassword456","UpdatedPassword456"));return true;}catch(AuthException e){return false;}}));
            start.countDown();int count=0;for(var result:results)if(result.get(10,TimeUnit.SECONDS))count++;assertEquals(1,count);assertEquals(1,users.findByEmailKey(email).orElseThrow().getAuthVersion());
        } finally {pool.shutdownNow();}
    }
    @Test void registrationContextRefreshAndVerificationAutoLoginUseRealSessionAndRotateCsrf() throws Exception {
        String email=email();var session=registerFlow(email);String before=session.getId();
        for(int i=0;i<2;i++)mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.maskedEmail").value(email.substring(0,2)+"•••@example.com")).andExpect(jsonPath("$.email").doesNotExist());
        var csrfToken=(org.springframework.security.web.csrf.CsrfToken)mvc.perform(get("/api/auth/csrf").session(session)).andReturn().getRequest().getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("customer"));
        assertNotEquals(before,session.getId());mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(get("/api/admin/auth-probe").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").session(session).header(csrfToken.getHeaderName(),csrfToken.getToken())).andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isConflict());
    }
    @Test void verificationWithoutContextAndArbitraryEmailCannotActivateOtherAccounts() throws Exception {
        String first=email(),second=email();var session=registerFlow(first);register(second);
        mvc.perform(get("/api/auth/verification-context")).andExpect(status().isConflict());
        flowPost("verify-email",null,Map.of("email",second,"code",code(second,AuthService.VERIFY))).andExpect(status().isConflict());
        String secondCode=code(second,AuthService.VERIFY);if(secondCode.equals(code(first,AuthService.VERIFY)))secondCode=secondCode.equals("000000")?"111111":"000000";
        flowPost("verify-email",session,Map.of("email",second,"code",secondCode)).andExpect(status().isBadRequest());
        flowPost("verify-email",session,Map.of("email",second,"code",code(first,AuthService.VERIFY))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(first));
        assertNull(users.findByEmailKey(second).orElseThrow().getEmailVerifiedAt());
    }
    @Test void verificationWrongExpiredExhaustedCodeAndWrongPurposeRemainPending() throws Exception {
        String email=email();var session=registerFlow(email);String actual=code(email,AuthService.VERIFY),wrong=actual.equals("000000")?"111111":"000000";
        for(int i=0;i<4;i++)flowPost("verify-email",session,Map.of("code",wrong)).andExpect(status().isBadRequest());
        flowPost("verify-email",session,Map.of("code",wrong)).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("CODE_ATTEMPTS_EXCEEDED"));
        clock.advance(601);flowPost("verify-email",session,Map.of("code",actual)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CODE_EXPIRED"));
        assertNull(users.findByEmailKey(email).orElseThrow().getEmailVerifiedAt());mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        flowPost("resend-verification",session,Map.of()).andExpect(status().isOk());
        flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isOk());
        flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isConflict());
    }
    @Test void verificationContextExpiresAndResendCannotChangeTarget() throws Exception {
        String first=email(),second=email();var session=registerFlow(first);register(second);
        flowPost("resend-verification",session,Map.of("email",second)).andExpect(status().isTooManyRequests());
        clock.advance(60);flowPost("resend-verification",session,Map.of("email",second)).andExpect(status().isOk());
        verify(mail,atLeast(2)).send(eq(first),anyString(),eq(AuthService.VERIFY));verify(mail,times(1)).send(eq(second),anyString(),eq(AuthService.VERIFY));
        clock.advance(1740);mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isConflict());
        flowPost("verify-email",session,Map.of("code",code(first,AuthService.VERIFY))).andExpect(status().isConflict());
    }
    @Test void pendingContextRecoveryRequiresCorrectPasswordAndNeverElevatesRole() throws Exception {
        String email=email();register(email);
        var wrong=new MockHttpSession();flowPost("login",wrong,Map.of("email",email,"password","WrongPassword123")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/verification-context").session(wrong)).andExpect(status().isConflict());
        var correct=new MockHttpSession();flowPost("login",correct,Map.of("email",email,"password",PASSWORD)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        mvc.perform(get("/api/auth/verification-context").session(correct)).andExpect(status().isOk());
        edit(email,u->u.setRole(UserRole.ADMIN));flowPost("verify-email",correct,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isConflict());
        assertNull(users.findByEmailKey(email).orElseThrow().getEmailVerifiedAt());
    }
    @Test void registrationMailFailureRetainsOwnedContextForSafeResend() throws Exception {
        String email=email();doThrow(new AuthException(503,"MAIL_DELIVERY_FAILED","Không thể gửi email.")).when(mail).send(eq(email),anyString(),anyString());
        var result=flowPost("register",null,Map.of("name","Test","email",email,"password",PASSWORD,"confirmPassword",PASSWORD)).andExpect(status().isServiceUnavailable()).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isOk());
        captureMail();flowPost("resend-verification",session,Map.of()).andExpect(status().isOk());flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isOk());
    }
    @Test void facebookCompletionCreatesItsOwnPendingContextAndStillUsesVerificationOtp() throws Exception {
        String email=email();var session=new MockHttpSession();session.setAttribute("OAUTH_PENDING",external("facebook",null,false));
        flowPost("oauth/complete",session,Map.of("name","Facebook user","email",email)).andExpect(status().isOk());
        assertNull(session.getAttribute("OAUTH_PENDING"));mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isOk());
        flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("customer"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
    }
    @Test void newFlowEndpointsKeepCsrfAndSixDigitValidation() throws Exception {
        String email=email();verified(email);var session=resetFlow(email);
        mvc.perform(post("/api/auth/verify-reset-code").session(session).contentType("application/json").content("{\"code\":\"123456\"}")).andExpect(status().isForbidden());
        flowPost("verify-reset-code",session,Map.of("code","12a456")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.code").exists());
        mvc.perform(post("/api/auth/resend-reset-code").session(session)).andExpect(status().isForbidden());
    }
    @Test void staleTabCannotUseAnotherResetFlowOrPendingRegistrationContext() throws Exception {
        String first=email(),second=email();verified(first);verified(second);var session=authorizedReset(first);
        String stale=((AuthFlowState.ResetFlow)session.getAttribute(AuthFlowState.RESET)).flowId();
        flowPost("forgot-password",session,Map.of("email",second)).andExpect(status().isAccepted());
        flowPost("verify-reset-code",session,Map.of("code",code(second,AuthService.RESET))).andExpect(status().isOk());
        flowPost("reset-password",session,Map.of("password","UpdatedPassword456","confirmPassword","UpdatedPassword456","flowId",stale)).andExpect(status().isConflict());
        assertNotNull(auth.authenticate(first,PASSWORD));assertNotNull(auth.authenticate(second,PASSWORD));
        resetFinish(session,"UpdatedPassword456","UpdatedPassword456",200);assertNotNull(auth.authenticate(second,"UpdatedPassword456"));
        String a=email(),b=email();var pending=registerFlow(a);String old=((AuthFlowState.Verification)pending.getAttribute(AuthFlowState.VERIFICATION)).contextId();
        flowPost("register",pending,Map.of("name","New pending","email",b,"password",PASSWORD,"confirmPassword",PASSWORD)).andExpect(status().isCreated());
        flowPost("verify-email",pending,Map.of("code",code(b,AuthService.VERIFY),"contextId",old)).andExpect(status().isConflict());
        assertNull(users.findByEmailKey(b).orElseThrow().getEmailVerifiedAt());
        flowPost("verify-email",pending,Map.of("code",code(b,AuthService.VERIFY))).andExpect(status().isOk());
    }
    @Test void abandonedFacebookCompletionCanRecoverOnlyThroughTheSameProviderSubject() throws Exception {
        String email=email();var identity=external("facebook",null,false);oauth.complete(identity,new AuthDtos.Complete("Facebook user",email));
        var session=new MockHttpSession();var request=new org.springframework.mock.web.MockHttpServletRequest();request.setSession(session);
        var response=new org.springframework.mock.web.MockHttpServletResponse();
        var principal=new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),Map.of("id",identity.subject(),"name","Facebook user"),"id");
        var token=new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(principal,principal.getAuthorities(),"facebook");
        oauthHandlers.onAuthenticationSuccess(request,response,token);
        assertEquals("http://localhost:5173/verify-email",response.getRedirectedUrl());
        mvc.perform(get("/api/auth/verification-context").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        flowPost("verify-email",session,Map.of("code",code(email,AuthService.VERIFY))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        assertNull(oauth.resolve(external("facebook",null,false),null));
    }
    @Test void resetFailurePathsAlwaysCompareHmacAndReturnTheSamePublicError() throws Exception {
        String known=email(),unknown=email(),pending=email(),locked=email();
        verified(known);register(pending);verified(locked);edit(locked,u->u.setIsActive(false));
        var session=resetFlow(known);
        String actual=code(known,AuthService.RESET),wrong=actual.equals("000000")?"111111":"000000";
        clearInvocations(mac);
        String generic=flowPost("verify-reset-code",session,Map.of("code",wrong)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CODE")).andReturn().getResponse().getContentAsString();
        verify(mac,times(1)).matches(any(AuthChallenge.class),eq(wrong));
        for(String address:List.of(unknown,pending,locked)) {
            var other=resetFlow(address);clearInvocations(mac);
            assertEquals(generic,flowPost("verify-reset-code",other,Map.of("code",wrong)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
            verify(mac,times(1)).matches(any(AuthChallenge.class),eq(wrong));
            assertNull(((AuthFlowState.ResetFlow)other.getAttribute(AuthFlowState.RESET)).permit());
        }
        for(int i=1;i<5;i++)flowPost("verify-reset-code",session,Map.of("code",wrong)).andExpect(status().isBadRequest());
        clearInvocations(mac);
        assertEquals(generic,flowPost("verify-reset-code",session,Map.of("code",actual)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        verify(mac,times(1)).matches(any(AuthChallenge.class),eq(actual));
        clock.advance(601);clearInvocations(mac);
        assertEquals(generic,flowPost("verify-reset-code",session,Map.of("code",actual)).andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        verify(mac,times(1)).matches(any(AuthChallenge.class),eq(actual));
        assertNotNull(auth.authenticate(known,PASSWORD));
    }
    @Test void resetVerificationRateLimitDoesNotDependOnAccountExistence() throws Exception {
        String known=email(),unknown=email();verified(known);var a=resetFlow(known);var b=resetFlow(unknown);
        String actual=code(known,AuthService.RESET),wrong=actual.equals("000000")?"111111":"000000";
        for(int i=0;i<20;i++)for(var session:List.of(a,b))
            flowPost("verify-reset-code",session,Map.of("code",wrong)).andExpect(status().isBadRequest());
        for(var session:List.of(a,b))flowPost("verify-reset-code",session,Map.of("code",wrong))
            .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(header().string("Retry-After","600"));
        assertNull(((AuthFlowState.ResetFlow)a.getAttribute(AuthFlowState.RESET)).permit());
        assertNull(((AuthFlowState.ResetFlow)b.getAttribute(AuthFlowState.RESET)).permit());
    }
    @Test void verificationResendRequiresItsSessionAndContextAndCsrf() throws Exception {
        String email=email();var session=registerFlow(email);
        mvc.perform(post("/api/auth/resend-verification").session(session).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        flowPost("resend-verification",null,Map.of("email",email)).andExpect(status().isConflict());
        flowPost("resend-verification",session,Map.of("contextId",UUID.randomUUID().toString(),"email",email))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERIFICATION_CONTEXT_EXPIRED"));
        assertNull(users.findByEmailKey(email).orElseThrow().getEmailVerifiedAt());
        verify(mail,times(1)).send(eq(email),anyString(),eq(AuthService.VERIFY));
    }
    @Test void resetKeepsBcryptCostAndUsesPlainVietnameseForByteLimitWithoutLosingPermit() throws Exception {
        String email=email();verified(email);var session=authorizedReset(email);
        String tooLong="đ".repeat(36)+"1";
        flowPost("reset-password",session,Map.of("password",tooLong,"confirmPassword",tooLong))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WEAK_PASSWORD"))
            .andExpect(jsonPath("$.message").value("Mật khẩu quá dài, vui lòng rút ngắn lại."));
        assertNotNull(((AuthFlowState.ResetFlow)session.getAttribute(AuthFlowState.RESET)).permit());
        String boundary="đ".repeat(35)+"a1"; // Exactly 72 UTF-8 bytes, still satisfies the unchanged policy.
        resetFinish(session,boundary,boundary,200);
        User user=users.findByEmailKey(email).orElseThrow();
        assertTrue(user.getPasswordHash().startsWith("$2a$12$"));assertTrue(passwords.matches(boundary,user.getPasswordHash()));
        assertEquals(1,user.getAuthVersion());assertTrue(session.isInvalid());
    }
}
