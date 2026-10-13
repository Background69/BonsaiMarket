package com.bonsaimarket.backend.auth;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import java.util.*;

@Component
public class OAuthRegistrations implements ClientRegistrationRepository,Iterable<ClientRegistration> {
    private final Map<String,ClientRegistration> registrations=new LinkedHashMap<>();
    public OAuthRegistrations(AuthSettings settings,
        @Value("${auth.google-client-id:}") String googleId, @Value("${auth.google-client-secret:}") String googleSecret,
        @Value("${auth.facebook-client-id:}") String facebookId, @Value("${auth.facebook-client-secret:}") String facebookSecret,
        @Value("${auth.facebook-api-version:}") String facebookVersion) {
        if(!googleId.isBlank()&&!googleSecret.isBlank()) {
            registrations.put("google",ClientRegistration.withRegistrationId("google").clientId(googleId).clientSecret(googleSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(settings.publicOrigin+"/login/oauth2/code/google").scope("openid","profile","email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs").issuerUri("https://accounts.google.com")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo").userNameAttributeName("sub").clientName("Google")
                .clientSettings(ClientRegistration.ClientSettings.builder().requireProofKey(true).build()).build());
        }
        // Pin a currently supported Graph API version from the Meta app dashboard; no guessed fallback.
        if(!facebookId.isBlank()&&!facebookSecret.isBlank()&&facebookVersion.matches("v[0-9]+\\.[0-9]+")) {
            registrations.put("facebook",ClientRegistration.withRegistrationId("facebook").clientId(facebookId).clientSecret(facebookSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(settings.publicOrigin+"/login/oauth2/code/facebook").scope("public_profile","email")
                .authorizationUri("https://www.facebook.com/"+facebookVersion+"/dialog/oauth")
                .tokenUri("https://graph.facebook.com/"+facebookVersion+"/oauth/access_token")
                .userInfoUri("https://graph.facebook.com/"+facebookVersion+"/me?fields=id,name,email")
                .userNameAttributeName("id").clientName("Facebook").build());
        }
    }
    @Override public ClientRegistration findByRegistrationId(String id) {return registrations.get(id);}
    @Override public Iterator<ClientRegistration> iterator() {return registrations.values().iterator();}
    public boolean configured(String provider) {return registrations.containsKey(provider);}
    public boolean any() {return !registrations.isEmpty();}
}
