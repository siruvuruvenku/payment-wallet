package com.example.wallet.repository;

import com.example.wallet.domain.LedgerEntry;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
    long countByTransferId(UUID transferId);
    long countByWalletId(UUID walletId);
}
