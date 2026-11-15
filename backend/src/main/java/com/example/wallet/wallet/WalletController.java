package com.example.wallet.wallet;

import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletService wallets;

    public WalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDtos.WalletResponse create(
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody WalletDtos.CreateRequest request) {
        return wallets.create(userId, request);
    }

    @GetMapping
    public List<WalletDtos.WalletResponse> list(@AuthenticationPrincipal UUID userId) {
        return wallets.list(userId);
    }

    @GetMapping("/{walletId}")
    public WalletDtos.WalletResponse get(@AuthenticationPrincipal UUID userId, @PathVariable UUID walletId) {
        return wallets.get(userId, walletId);
    }

    @GetMapping("/{walletId}/balance")
    public WalletDtos.BalanceResponse balance(@AuthenticationPrincipal UUID userId, @PathVariable UUID walletId) {
        return wallets.balance(userId, walletId);
    }

    @GetMapping("/{walletId}/ledger")
    public Page<WalletDtos.LedgerEntryResponse> ledger(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return wallets.ledger(userId, walletId, page, size);
    }
}
