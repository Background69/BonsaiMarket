package com.bonsaimarket.backend.auth;

import java.io.Serializable;
import java.security.Principal;
import com.bonsaimarket.backend.user.User;

public record AuthPrincipal(Long id,long version) implements Principal, Serializable {
    @Override public String getName() {return id.toString();}
    public static AuthPrincipal of(User user) {return new AuthPrincipal(user.getId(),user.getAuthVersion());}
}
