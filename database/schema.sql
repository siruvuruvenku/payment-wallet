CREATE DATABASE IF NOT EXISTS wallet
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE wallet;

CREATE TABLE IF NOT EXISTS app_users (
    id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uq_app_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS wallets (
    id BINARY(16) NOT NULL,
    owner_id BINARY(16) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_wallets PRIMARY KEY (id),
    CONSTRAINT fk_wallets_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_status CHECK (status IN ('ACTIVE', 'FROZEN')),
    INDEX idx_wallets_owner_created (owner_id, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS transfers (
    id BINARY(16) NOT NULL,
    source_wallet_id BINARY(16) NOT NULL,
    destination_wallet_id BINARY(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transfers_destination FOREIGN KEY (destination_wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_transfer_distinct_wallets CHECK (source_wallet_id <> destination_wallet_id),
    INDEX idx_transfers_source_created (source_wallet_id, created_at),
    INDEX idx_transfers_destination_created (destination_wallet_id, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ledger_entries (
    id BINARY(16) NOT NULL,
    transfer_id BINARY(16) NOT NULL,
    wallet_id BINARY(16) NOT NULL,
    entry_type VARCHAR(8) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id),
    CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
    CONSTRAINT uq_ledger_transfer_entry UNIQUE (transfer_id, entry_type),
    INDEX idx_ledger_transfer (transfer_id),
    INDEX idx_ledger_wallet_created (wallet_id, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS idempotency_records (
    id BINARY(16) NOT NULL,
    owner_id BINARY(16) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    transfer_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_idempotency_records PRIMARY KEY (id),
    CONSTRAINT fk_idempotency_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT fk_idempotency_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT uq_idempotency_owner_key UNIQUE (owner_id, idempotency_key)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) NOT NULL,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    INDEX idx_outbox_unpublished (published_at, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BINARY(16) NOT NULL,
    actor_user_id BINARY(16) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BINARY(16) NULL,
    details JSON NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_users(id),
    INDEX idx_audit_actor_created (actor_user_id, created_at),
    INDEX idx_audit_entity_created (entity_type, entity_id, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reconciliation_records (
    id BINARY(16) NOT NULL,
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
    CONSTRAINT pk_reconciliation_records PRIMARY KEY (id),
    CONSTRAINT fk_reconciliation_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT chk_reconciliation_status CHECK (status IN ('MATCHED', 'MISMATCH', 'INCOMPLETE')),
    INDEX idx_reconciliation_run_status (run_id, status),
    INDEX idx_reconciliation_transfer (transfer_id)
) ENGINE = InnoDB;
