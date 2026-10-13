package com.bonsaimarket.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<AuthChallenge,Long> {
    Optional<AuthChallenge> findByUserIdAndPurpose(Long userId, String purpose);
}
