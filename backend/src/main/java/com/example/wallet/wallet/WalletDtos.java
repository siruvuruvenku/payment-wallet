package com.example.wallet.wallet;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public final class WalletDtos {
    private WalletDtos() {}

    public record CreateRequest(
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO-style 3-letter currency code")
                    String currency) {}

    public record WalletResponse(UUID walletId, String currency, BigDecimal balance, WalletStatus status) {
        public static WalletResponse from(Wallet wallet) {
            return new WalletResponse(wallet.getId(), wallet.getCurrency(), wallet.getBalance(), wallet.getStatus());
        }
    }

    public record BalanceResponse(UUID walletId, String currency, BigDecimal availableBalance) {}

    public record LedgerEntryResponse(
            UUID entryId, UUID transferId, UUID walletId, String type, BigDecimal amount, String currency, java.time.Instant createdAt) {
        public static LedgerEntryResponse from(com.example.wallet.domain.LedgerEntry entry) {
            return new LedgerEntryResponse(entry.getId(), entry.getTransferId(), entry.getWalletId(),
                    entry.getEntryType().name(), entry.getAmount(), entry.getCurrency(), entry.getCreatedAt());
        }
    }
}
