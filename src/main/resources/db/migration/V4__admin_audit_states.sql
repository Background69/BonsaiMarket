-- Optional transition evidence for new actions. Historical audits are not backfilled.
CREATE TABLE marketplace_audit_states (
 audit_id BIGINT PRIMARY KEY,
 before_state VARCHAR(160) NULL,
 after_state VARCHAR(160) NULL,
 CONSTRAINT fk_audit_state_entry FOREIGN KEY(audit_id) REFERENCES marketplace_audit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
