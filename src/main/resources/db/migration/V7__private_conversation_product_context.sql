-- Optional, authoritative product context. Existing conversations remain unchanged.
ALTER TABLE private_conversations ADD COLUMN product_id BIGINT NULL;
ALTER TABLE private_conversations ADD CONSTRAINT fk_private_conversation_product FOREIGN KEY (product_id) REFERENCES products(id);
