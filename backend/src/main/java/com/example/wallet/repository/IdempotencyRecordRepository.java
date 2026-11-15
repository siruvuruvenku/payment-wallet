package com.example.wallet.repository;

import com.example.wallet.domain.IdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByOwnerIdAndIdempotencyKey(UUID ownerId, String idempotencyKey);
}
