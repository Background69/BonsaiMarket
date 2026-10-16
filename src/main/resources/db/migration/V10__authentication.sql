-- Additive auth only. Existing accounts retain their roles/data and need email verification.
ALTER TABLE users
    ADD COLUMN email_verified_at DATETIME(6) NULL,
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN email_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
        GENERATED ALWAYS AS (LOWER(TRIM(email))) STORED,
    ADD CONSTRAINT uk_users_email_key UNIQUE (email_key);

CREATE TABLE auth_challenges (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    nonce VARCHAR(36) NOT NULL,
    code_mac VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    attempts INT NOT NULL DEFAULT 0,
    consumed_at DATETIME(6) NULL,
    CONSTRAINT uk_auth_challenge UNIQUE (user_id, purpose),
    CONSTRAINT fk_auth_challenge_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_auth_challenge_purpose CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD')),
    CONSTRAINT chk_auth_challenge_attempts CHECK (attempts BETWEEN 0 AND 5),
    INDEX idx_auth_challenge_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE oauth_identities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provider_subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_oauth_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_oauth_user_provider UNIQUE (user_id, provider),
    CONSTRAINT fk_oauth_identity_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_oauth_provider CHECK (provider IN ('google', 'facebook'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
