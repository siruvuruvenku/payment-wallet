CREATE TABLE audit_logs (
    id BINARY(16) PRIMARY KEY,
    actor_user_id BINARY(16) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BINARY(16) NULL,
    details JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users(id),
    INDEX idx_audit_actor_created (actor_user_id, created_at),
    INDEX idx_audit_entity_created (entity_type, entity_id, created_at)
);

CREATE TABLE reconciliation_records (
    id BINARY(16) PRIMARY KEY,
    run_id BINARY(16) NOT NULL,
    transfer_id BINARY(16) NULL,
    check_type VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expected_debit DECIMAL(19,2) NULL,
    actual_debit DECIMAL(19,2) NULL,
    expected_credit DECIMAL(19,2) NULL,
    actual_credit DECIMAL(19,2) NULL,
    details JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reconciliation_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT chk_reconciliation_status CHECK (status IN ('MATCHED', 'MISMATCH', 'INCOMPLETE')),
    INDEX idx_reconciliation_run_status (run_id, status),
    INDEX idx_reconciliation_transfer (transfer_id)
);
