package com.bonsaimarket.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdentityRepository extends JpaRepository<OAuthIdentity,Long> {
    Optional<OAuthIdentity> findByProviderAndProviderSubject(String provider,String providerSubject);
    boolean existsByUserIdAndProvider(Long userId,String provider);
}
