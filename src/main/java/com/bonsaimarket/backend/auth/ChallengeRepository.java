package com.bonsaimarket.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<AuthChallenge,Long> {
    Optional<AuthChallenge> findByUserIdAndPurpose(Long userId, String purpose);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AuthChallenge c where c.userId = :userId and c.purpose = :purpose")
    Optional<AuthChallenge> lockByUserIdAndPurpose(Long userId, String purpose);
}
