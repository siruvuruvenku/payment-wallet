package com.example.wallet.wallet;

import com.example.wallet.api.ApiException;
import com.example.wallet.domain.Wallet;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.WalletRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final WalletRepository wallets;
    private final LedgerEntryRepository ledger;

    public WalletService(WalletRepository wallets, LedgerEntryRepository ledger) {
        this.wallets = wallets;
        this.ledger = ledger;
    }

    @Transactional
    public WalletDtos.WalletResponse create(UUID ownerId, WalletDtos.CreateRequest request) {
        return WalletDtos.WalletResponse.from(wallets.save(new Wallet(ownerId, request.currency())));
    }

    @Transactional(readOnly = true)
    public List<WalletDtos.WalletResponse> list(UUID ownerId) {
        return wallets.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(WalletDtos.WalletResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WalletDtos.WalletResponse get(UUID ownerId, UUID walletId) {
        return WalletDtos.WalletResponse.from(ownedWallet(ownerId, walletId));
    }

    @Transactional(readOnly = true)
    public WalletDtos.BalanceResponse balance(UUID ownerId, UUID walletId) {
        Wallet wallet = ownedWallet(ownerId, walletId);
        return new WalletDtos.BalanceResponse(wallet.getId(), wallet.getCurrency(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public Page<WalletDtos.LedgerEntryResponse> ledger(UUID ownerId, UUID walletId, int page, int size) {
        ownedWallet(ownerId, walletId);
        PageRequest request = pageRequest(page, size);
        return ledger.findByWalletIdOrderByCreatedAtDesc(walletId, request).map(WalletDtos.LedgerEntryResponse::from);
    }

    private Wallet ownedWallet(UUID ownerId, UUID walletId) {
        return wallets.findByIdAndOwnerId(walletId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    public static PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size);
    }
}
