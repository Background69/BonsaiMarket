package com.bonsaimarket.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OAuthProviderContractTest {
    private final AuthSettings settings = new AuthSettings("", "", "", "https://shop.example.invalid", "https://auth.example.invalid");

    @Test void partialProviderConfigurationNeverAdvertisesAnUnusableRegistration() {
        for (String[] values : List.of(
            new String[]{"", "secret", "", "", ""},
            new String[]{"id", "", "", "", ""},
            new String[]{"", "", "id", "", "v99.0"},
            new String[]{"", "", "", "secret", "v99.0"},
            new String[]{"", "", "id", "secret", ""},
            new String[]{"", "", "id", "secret", "latest"})) {
            var registry = new OAuthRegistrations(settings, values[0], values[1], values[2], values[3], values[4]);
            assertFalse(registry.any());
            assertNull(registry.findByRegistrationId("google"));
            assertNull(registry.findByRegistrationId("facebook"));
        }
    }

    @Test void googleUsesBackendCallbackOidcIssuerAndProofKey() {
        var registry = new OAuthRegistrations(settings, "id", "secret", "", "", "");
        var google = registry.findByRegistrationId("google");
        assertTrue(registry.configured("google"));
        assertFalse(registry.configured("facebook"));
        assertEquals("https://auth.example.invalid/login/oauth2/code/google", google.getRedirectUri());
        assertEquals("https://accounts.google.com", google.getProviderDetails().getIssuerUri());
        assertEquals("sub", google.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        assertTrue(google.getScopes().containsAll(List.of("openid", "profile", "email")));
        assertTrue(google.getClientSettings().isRequireProofKey());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, google.getAuthorizationGrantType());
    }

    @Test void facebookUsesExplicitVersionCodeFlowAndProviderSubject() {
        var registry = new OAuthRegistrations(settings, "", "", "id", "secret", "v99.0");
        var facebook = registry.findByRegistrationId("facebook");
        assertTrue(registry.configured("facebook"));
        assertFalse(registry.configured("google"));
        assertEquals("https://auth.example.invalid/login/oauth2/code/facebook", facebook.getRedirectUri());
        assertEquals("https://www.facebook.com/v99.0/dialog/oauth", facebook.getProviderDetails().getAuthorizationUri());
        assertEquals("https://graph.facebook.com/v99.0/oauth/access_token", facebook.getProviderDetails().getTokenUri());
        assertEquals("id", facebook.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        assertEquals(ClientAuthenticationMethod.CLIENT_SECRET_POST, facebook.getClientAuthenticationMethod());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, facebook.getAuthorizationGrantType());
        assertFalse(registry.configured("apple"));
    }
}
