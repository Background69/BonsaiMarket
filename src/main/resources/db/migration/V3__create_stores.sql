CREATE TABLE stores
(
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT       NOT NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT NULL,
    logo_url      VARCHAR(500) NULL,
    phone         VARCHAR(30) NULL,
    address       VARCHAR(500) NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_stores_owner UNIQUE (owner_user_id),
    CONSTRAINT fk_stores_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT chk_stores_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
