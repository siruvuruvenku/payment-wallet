package com.example.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "transfer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID transferId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(UUID ownerId, String key, String requestHash, UUID transferId) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.idempotencyKey = key;
        this.requestHash = requestHash;
        this.transferId = transferId;
    }

    public String getRequestHash() { return requestHash; }
    public UUID getTransferId() { return transferId; }
}
