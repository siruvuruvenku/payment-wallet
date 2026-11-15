CREATE TABLE app_users (
    id BINARY(16) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE wallets (
    id BINARY(16) PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_wallets_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT chk_wallets_balance CHECK (balance >= 0),
    CONSTRAINT chk_wallets_status CHECK (status IN ('ACTIVE', 'FROZEN')),
    INDEX idx_wallets_owner_created (owner_id, created_at)
);

CREATE TABLE transfers (
    id BINARY(16) PRIMARY KEY,
    source_wallet_id BINARY(16) NOT NULL,
    destination_wallet_id BINARY(16) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_wallet_id) REFERENCES wallets(id),
    CONSTRAINT fk_transfers_destination FOREIGN KEY (destination_wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_transfer_distinct_wallets CHECK (source_wallet_id <> destination_wallet_id),
    INDEX idx_transfers_source_created (source_wallet_id, created_at),
    INDEX idx_transfers_destination_created (destination_wallet_id, created_at)
);

CREATE TABLE ledger_entries (
    id BINARY(16) PRIMARY KEY,
    transfer_id BINARY(16) NOT NULL,
    wallet_id BINARY(16) NOT NULL,
    entry_type VARCHAR(8) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT chk_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount CHECK (amount > 0),
    INDEX idx_ledger_wallet_created (wallet_id, created_at),
    INDEX idx_ledger_transfer (transfer_id)
);

CREATE TABLE idempotency_records (
    id BINARY(16) PRIMARY KEY,
    owner_id BINARY(16) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    transfer_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_idempotency_owner FOREIGN KEY (owner_id) REFERENCES app_users(id),
    CONSTRAINT fk_idempotency_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id),
    CONSTRAINT uq_idempotency_owner_key UNIQUE (owner_id, idempotency_key)
);

CREATE TABLE outbox_events (
    id BINARY(16) PRIMARY KEY,
    aggregate_id BINARY(16) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at TIMESTAMP(6) NULL,
    INDEX idx_outbox_unpublished (published_at, created_at)
);
