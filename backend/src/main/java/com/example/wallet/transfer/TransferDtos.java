package com.example.wallet.transfer;

import com.example.wallet.domain.Transfer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransferDtos {
    private TransferDtos() {}

    public record CreateRequest(
            @NotNull UUID sourceWalletId,
            @NotNull UUID destinationWalletId,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency) {}

    public record TransferResponse(
            UUID transferId,
            UUID sourceWalletId,
            UUID destinationWalletId,
            BigDecimal amount,
            String currency,
            String status,
            Instant createdAt) {
        public static TransferResponse from(Transfer transfer) {
            return new TransferResponse(transfer.getId(), transfer.getSourceWalletId(), transfer.getDestinationWalletId(),
                    transfer.getAmount(), transfer.getCurrency(), transfer.getStatus().name(), transfer.getCreatedAt());
        }
    }
}
