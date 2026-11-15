package com.example.wallet.transfer;

import com.example.wallet.api.ApiException;
import com.example.wallet.domain.EntryType;
import com.example.wallet.domain.IdempotencyRecord;
import com.example.wallet.domain.LedgerEntry;
import com.example.wallet.domain.OutboxEvent;
import com.example.wallet.domain.Transfer;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletStatus;
import com.example.wallet.repository.IdempotencyRecordRepository;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.OutboxEventRepository;
import com.example.wallet.repository.TransferRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.wallet.WalletService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final TransferRepository transfers;
    private final LedgerEntryRepository ledger;
    private final IdempotencyRecordRepository idempotency;
    private final OutboxEventRepository outbox;

    public TransferService(
            UserRepository users,
            WalletRepository wallets,
            TransferRepository transfers,
            LedgerEntryRepository ledger,
            IdempotencyRecordRepository idempotency,
            OutboxEventRepository outbox) {
        this.users = users;
        this.wallets = wallets;
        this.transfers = transfers;
        this.ledger = ledger;
        this.idempotency = idempotency;
        this.outbox = outbox;
    }

    @Transactional
    public TransferDtos.TransferResponse create(UUID ownerId, String key, TransferDtos.CreateRequest request) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 128 characters");
        }

        users.findByIdForUpdate(ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account is unavailable"));

        String requestHash = requestHash(request);
        var existing = idempotency.findByOwnerIdAndIdempotencyKey(ownerId, key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "Idempotency-Key was already used with different input");
            }
            Transfer original = transfers.findById(existing.get().getTransferId())
                    .orElseThrow(() -> new IllegalStateException("Idempotency record references a missing transfer"));
            return TransferDtos.TransferResponse.from(original);
        }

        if (request.sourceWalletId().equals(request.destinationWalletId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Source and destination wallets must differ");
        }

        List<Wallet> lockedWallets = wallets.findAllByIdInOrderByIdForUpdate(
                List.of(request.sourceWalletId(), request.destinationWalletId()));
        if (lockedWallets.size() != 2) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Source or destination wallet not found");
        }
        Wallet source = lockedWallets.stream()
                .filter(wallet -> wallet.getId().equals(request.sourceWalletId())).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Source wallet not found"));
        Wallet destination = lockedWallets.stream()
                .filter(wallet -> wallet.getId().equals(request.destinationWalletId())).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Destination wallet not found"));

        if (!source.getOwnerId().equals(ownerId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Source wallet not found");
        }
        if (source.getStatus() != WalletStatus.ACTIVE || destination.getStatus() != WalletStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Both wallets must be active");
        }
        if (!source.getCurrency().equals(request.currency()) || !destination.getCurrency().equals(request.currency())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Wallet currencies must match the transfer currency");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient wallet balance");
        }

        source.debit(request.amount());
        destination.credit(request.amount());
        Transfer transfer = transfers.save(new Transfer(
                source.getId(), destination.getId(), request.amount(), request.currency()));
        ledger.saveAll(List.of(
                new LedgerEntry(transfer.getId(), source.getId(), EntryType.DEBIT, request.amount(), request.currency()),
                new LedgerEntry(transfer.getId(), destination.getId(), EntryType.CREDIT, request.amount(), request.currency())));
        idempotency.save(new IdempotencyRecord(ownerId, key, requestHash, transfer.getId()));
        outbox.save(new OutboxEvent(transfer.getId(), "TransferCompleted", Map.of(
                "transferId", transfer.getId().toString(),
                "amount", request.amount().toPlainString(),
                "currency", request.currency())));
        return TransferDtos.TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferDtos.TransferResponse get(UUID ownerId, UUID transferId) {
        List<UUID> walletIds = wallets.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(Wallet::getId).toList();
        if (walletIds.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Transfer not found");
        }
        Transfer transfer = transfers.findOwnedTransfer(transferId, walletIds)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transfer not found"));
        return TransferDtos.TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public Page<TransferDtos.TransferResponse> history(UUID ownerId, int page, int size) {
        PageRequest request = WalletService.pageRequest(page, size);
        List<UUID> walletIds = wallets.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(Wallet::getId).toList();
        if (walletIds.isEmpty()) {
            return Page.empty(request);
        }
        return transfers.findBySourceWalletIdInOrDestinationWalletIdInOrderByCreatedAtDesc(walletIds, walletIds, request)
                .map(TransferDtos.TransferResponse::from);
    }

    private String requestHash(TransferDtos.CreateRequest request) {
        String canonical = request.sourceWalletId() + "|" + request.destinationWalletId() + "|"
                + request.amount().stripTrailingZeros().toPlainString() + "|" + request.currency();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
