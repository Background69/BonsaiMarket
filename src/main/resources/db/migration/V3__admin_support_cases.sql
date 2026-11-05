-- Additive Admin support case management. No financial transitions or historical backfill.
-- Apply only after reviewing V1/V2 checksums and rehearsing the target upgrade.
CREATE TABLE support_cases (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT NOT NULL,
 opened_by BIGINT NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
 revision BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_support_order UNIQUE(order_id),
 CONSTRAINT fk_support_order FOREIGN KEY(order_id) REFERENCES orders(id),
 CONSTRAINT fk_support_opener FOREIGN KEY(opened_by) REFERENCES users(id),
 CONSTRAINT ck_support_status CHECK(status IN ('OPEN','IN_REVIEW','RESOLVED','CLOSED')),
 CONSTRAINT ck_support_revision CHECK(revision>=0),
 INDEX idx_support_queue(status,updated_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE support_case_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 case_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 event_type VARCHAR(20) NOT NULL,
 body VARCHAR(4000) NOT NULL,
 request_key VARCHAR(80) NOT NULL,
 status VARCHAR(20) NOT NULL,
 revision BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 CONSTRAINT fk_support_event_case FOREIGN KEY(case_id) REFERENCES support_cases(id),
 CONSTRAINT fk_support_event_actor FOREIGN KEY(actor_id) REFERENCES users(id),
 CONSTRAINT uq_support_event_request UNIQUE(case_id,actor_id,request_key),
 CONSTRAINT uq_support_event_revision UNIQUE(case_id,revision),
 CONSTRAINT ck_support_event_kind CHECK(event_type IN ('OPENED','RESPONSE','DECISION')),
 CONSTRAINT ck_support_event_status CHECK(status IN ('OPEN','IN_REVIEW','RESOLVED','CLOSED')),
 CONSTRAINT ck_support_event_revision CHECK(revision>=0),
 INDEX idx_support_event_time(case_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
