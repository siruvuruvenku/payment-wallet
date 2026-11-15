package com.example.wallet.repository;

import com.example.wallet.domain.Transfer;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Page<Transfer> findBySourceWalletIdInOrDestinationWalletIdInOrderByCreatedAtDesc(
            Collection<UUID> sourceWalletIds, Collection<UUID> destinationWalletIds, Pageable pageable);

    @Query("select t from Transfer t where t.id = :transferId and "
            + "(t.sourceWalletId in :walletIds or t.destinationWalletId in :walletIds)")
    Optional<Transfer> findOwnedTransfer(@Param("transferId") UUID transferId, @Param("walletIds") Collection<UUID> walletIds);
}
