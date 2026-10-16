package com.bonsaimarket.backend.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Base64;
import java.time.*;

class AuthConfigurationTest {
    AuthSettings settings(String key,String host,String from) {return new AuthSettings(key,from,host,"http://localhost:5173","http://localhost:5173");}
    @Test void missingDeliveryKeyAndProvidersFailClosed() {
        var settings=settings("","","");assertEquals("AUTH_NOT_CONFIGURED",assertThrows(AuthException.class,settings::requireDelivery).code);
        var providers=new OAuthRegistrations(settings,"","","","","");assertFalse(providers.any());assertFalse(providers.configured("google"));assertFalse(providers.configured("facebook"));
    }
    @Test void invalidKeyAndInsecureProductionOriginsRejected() {
        assertThrows(IllegalStateException.class,()->settings("not-base64","smtp.invalid","test@example.com"));
        assertThrows(IllegalStateException.class,()->settings(Base64.getEncoder().encodeToString(new byte[16]),"smtp.invalid","test@example.com"));
        assertThrows(IllegalStateException.class,()->new AuthSettings("","","","http://example.com","http://example.com"));
    }
    @Test void providerConfigurationRequiresCompleteCredentialsAndExplicitFacebookVersion() {
        var settings=settings("","","");var partial=new OAuthRegistrations(settings,"id","","fb","secret","");assertFalse(partial.any());
        var configured=new OAuthRegistrations(settings,"id","secret","fb","secret","v99.0");assertTrue(configured.configured("google"));assertTrue(configured.configured("facebook"));
        assertEquals("http://localhost:5173/login/oauth2/code/google",configured.findByRegistrationId("google").getRedirectUri());
        assertTrue(configured.findByRegistrationId("google").getClientSettings().isRequireProofKey());
    }
    @Test void ipLimiterSurvivesRotatingEmailKeysAndExpiresWindow() {
        var clock=new AuthIntegrationTest.MutableClock();var limits=new AuthRateLimiter(clock);
        for(int i=0;i<3;i++) limits.check("login","user"+i,"same-peer",10,3,60);
        assertEquals(429,assertThrows(AuthException.class,()->limits.check("login","new-email","same-peer",10,3,60)).status);
        clock.advance(61);assertDoesNotThrow(()->limits.check("login","new-email","same-peer",10,3,60));
    }
    @Test void hmacBindsAccountPurposeAndNonce() {
        var settings=settings(Base64.getEncoder().encodeToString(new byte[32]),"smtp.invalid","test@example.com");var mac=new OtpMac(settings);var c=new AuthChallenge();
        c.setUserId(1L);c.setPurpose(AuthService.VERIFY);c.setNonce("nonce-one");String first=mac.hash(c,"123456");c.setCodeMac(first);assertTrue(mac.matches(c,"123456"));
        c.setNonce("nonce-two");assertFalse(mac.matches(c,"123456"));c.setNonce("nonce-one");c.setPurpose(AuthService.RESET);assertFalse(mac.matches(c,"123456"));c.setPurpose(AuthService.VERIFY);c.setUserId(2L);assertFalse(mac.matches(c,"123456"));
    }
    @Test void unconfiguredProviderFailsClosedEvenWhenAnotherIsConfigured() throws Exception {
        var providers=new OAuthRegistrations(settings("","",""),"id","secret","","","");
        var filter=new OAuthAvailabilityFilter(providers);var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/oauth2/authorization/facebook");
        var response=new org.springframework.mock.web.MockHttpServletResponse();var chain=org.mockito.Mockito.mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(request,response,chain);assertEquals(503,response.getStatus());assertTrue(response.getContentAsString().contains("OAUTH_NOT_CONFIGURED"));org.mockito.Mockito.verifyNoInteractions(chain);
    }
}
