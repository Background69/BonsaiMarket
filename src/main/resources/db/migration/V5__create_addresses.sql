CREATE TABLE addresses
(
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    label          VARCHAR(80)  NOT NULL,
    recipient_name VARCHAR(150) NOT NULL,
    phone          VARCHAR(30)  NOT NULL,
    province_name  VARCHAR(150) NOT NULL,
    province_id    VARCHAR(30) NULL,
    district_name  VARCHAR(150) NOT NULL,
    district_id    VARCHAR(30) NULL,
    ward_name      VARCHAR(150) NOT NULL,
    ward_code      VARCHAR(30) NULL,
    address_detail VARCHAR(500) NOT NULL,
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX          idx_addresses_user (user_id),
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
