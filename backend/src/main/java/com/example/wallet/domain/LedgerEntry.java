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
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "transfer_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID transferId;

    @Column(name = "wallet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 8)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(UUID transferId, UUID walletId, EntryType entryType, BigDecimal amount, String currency) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        this.walletId = walletId;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public UUID getWalletId() { return walletId; }
    public EntryType getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
