package com.example.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {}

    public Wallet(UUID ownerId, String currency) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.status = WalletStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public WalletStatus getStatus() { return status; }

    public void debit(BigDecimal amount) { balance = balance.subtract(amount); }
    public void credit(BigDecimal amount) { balance = balance.add(amount); }
}
