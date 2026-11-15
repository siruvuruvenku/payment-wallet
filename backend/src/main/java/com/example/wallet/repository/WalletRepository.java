package com.example.wallet.repository;

import com.example.wallet.domain.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    List<Wallet> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id in :ids order by w.id")
    List<Wallet> findAllByIdInOrderByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
