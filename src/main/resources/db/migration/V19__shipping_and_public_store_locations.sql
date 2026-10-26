-- Additive only. No legacy address conversion, seed, or financial state changes.
CREATE TABLE store_public_locations (
    store_id BIGINT NOT NULL PRIMARY KEY,
    mode VARCHAR(10) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    region VARCHAR(120) NOT NULL,
    ward VARCHAR(120) NOT NULL,
    public_address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    consent_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_public_location_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT chk_location_mode CHECK (mode IN ('AREA','EXACT')),
    CONSTRAINT chk_location_consent CHECK (published=FALSE OR consent_at IS NOT NULL),
    CONSTRAINT chk_location_coordinates CHECK ((mode='AREA' AND latitude IS NULL AND longitude IS NULL AND public_address='') OR
        (mode='EXACT' AND latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND NOT (latitude=0 AND longitude=0))),
    INDEX idx_public_locations_visibility (published,store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- A seller measures the complete packed order. Never infer a box from plant dimensions.
CREATE TABLE shipping_parcels (
    order_id BIGINT NOT NULL PRIMARY KEY,
    weight_grams INT NOT NULL,
    length_cm INT NOT NULL,
    width_cm INT NOT NULL,
    height_cm INT NOT NULL,
    carrier_accepted_goods BOOLEAN NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_shipping_parcel_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT chk_shipping_parcel CHECK (weight_grams BETWEEN 1 AND 19999 AND length_cm BETWEEN 1 AND 200 AND width_cm BETWEEN 1 AND 200 AND height_cm BETWEEN 1 AND 200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipping_quotes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    shop_id INT NOT NULL,
    parcel_revision BIGINT NOT NULL,
    service_type INT NOT NULL,
    service_name VARCHAR(120) NOT NULL,
    fee DECIMAL(15,2) NOT NULL,
    order_fingerprint VARCHAR(64) NOT NULL,
    destination_snapshot TEXT NOT NULL,
    pickup_snapshot TEXT NOT NULL,
    parcel_snapshot TEXT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    selected_at DATETIME(6) NULL,
    CONSTRAINT fk_shipping_quote_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipping_quote_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    CONSTRAINT uk_shipping_quote_order UNIQUE (id,order_id),
    CONSTRAINT chk_shipping_quote_fee CHECK (fee>=0),
    INDEX idx_shipping_quotes_order (order_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipments (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    quote_id VARCHAR(36) NOT NULL,
    shop_id INT NOT NULL,
    client_reference VARCHAR(50) NOT NULL,
    tracking_code VARCHAR(50) NULL,
    state VARCHAR(30) NOT NULL,
    last_event_at DATETIME(6) NULL,
    last_sync_at DATETIME(6) NULL,
    sync_after DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_shipment_order UNIQUE (order_id),
    CONSTRAINT uk_shipment_reference UNIQUE (client_reference),
    CONSTRAINT uk_shipment_tracking UNIQUE (tracking_code),
    CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipment_quote FOREIGN KEY (quote_id,order_id) REFERENCES shipping_quotes(id,order_id),
    CONSTRAINT chk_shipment_state CHECK (state IN ('CREATING','UNKNOWN','WAITING_PICKUP','PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERY_FAILED','DELIVERED','RETURN_REQUESTED','RETURNING','RETURN_FAILED','RETURNED','CANCELLED','EXCEPTION','LOST','DAMAGED','DISPOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE shipment_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id VARCHAR(36) NOT NULL,
    dedup_key VARCHAR(64) NOT NULL,
    carrier_status VARCHAR(40) NOT NULL,
    state VARCHAR(30) NOT NULL,
    event_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    source VARCHAR(12) NOT NULL,
    CONSTRAINT fk_shipment_event FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT uk_shipment_event UNIQUE (shipment_id,dedup_key),
    INDEX idx_shipment_events_time (shipment_id,event_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
