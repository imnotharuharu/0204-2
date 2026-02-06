CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    user_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(64),
    message VARCHAR(200),
    created_at TIMESTAMP
);
