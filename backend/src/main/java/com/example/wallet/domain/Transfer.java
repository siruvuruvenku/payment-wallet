package com.example.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transfers")
public class Transfer {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "source_wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID sourceWalletId;

    @Column(name = "destination_wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID destinationWalletId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {}

    public Transfer(UUID sourceWalletId, UUID destinationWalletId, BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.sourceWalletId = sourceWalletId;
        this.destinationWalletId = destinationWalletId;
        this.amount = amount;
        this.currency = currency;
        this.status = TransferStatus.COMPLETED;
    }

    public UUID getId() { return id; }
    public UUID getSourceWalletId() { return sourceWalletId; }
    public UUID getDestinationWalletId() { return destinationWalletId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
