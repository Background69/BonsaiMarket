-- Extend the existing outbox for persisted in-app notifications. EMAIL is unchanged.
ALTER TABLE order_notification_outbox ADD COLUMN channel VARCHAR(10) NOT NULL DEFAULT 'EMAIL';
ALTER TABLE order_notification_outbox ADD COLUMN recipient_user_id BIGINT NULL;
ALTER TABLE order_notification_outbox ADD COLUMN read_at DATETIME(6) NULL;
ALTER TABLE order_notification_outbox ADD COLUMN target_link VARCHAR(500) NULL;
ALTER TABLE order_notification_outbox ADD CONSTRAINT fk_notification_recipient FOREIGN KEY(recipient_user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE order_notification_outbox ADD CONSTRAINT chk_notification_channel CHECK(channel IN ('EMAIL','IN_APP'));
ALTER TABLE order_notification_outbox DROP CHECK chk_notification_target;
ALTER TABLE order_notification_outbox ADD CONSTRAINT chk_notification_target CHECK (
 (channel='IN_APP' AND recipient_user_id IS NOT NULL AND target_link IS NOT NULL)
 OR (channel='EMAIL' AND (order_id IS NOT NULL OR event_type IN ('PRODUCT_QUESTION_ASKED','PRODUCT_QUESTION_ANSWERED','PRODUCT_QUESTION_REPLIED','PRODUCT_QUESTION_MODERATED','COMMUNITY_COMMENTED','COMMUNITY_ACCEPTED','COMMUNITY_MODERATED')))
);
CREATE INDEX idx_notification_inbox ON order_notification_outbox(channel,recipient_user_id,read_at,created_at,id);
