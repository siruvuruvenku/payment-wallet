package com.example.wallet.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.wallet.api.ApiException;
import com.example.wallet.domain.IdempotencyRecord;
import com.example.wallet.domain.Transfer;
import com.example.wallet.domain.Wallet;
import com.example.wallet.repository.IdempotencyRecordRepository;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.OutboxEventRepository;
import com.example.wallet.repository.TransferRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock private UserRepository users;
    @Mock private WalletRepository wallets;
    @Mock private TransferRepository transfers;
    @Mock private LedgerEntryRepository ledger;
    @Mock private IdempotencyRecordRepository idempotency;
    @Mock private OutboxEventRepository outbox;
    @InjectMocks private TransferService service;

    private UUID ownerId;
    private UUID sourceId;
    private UUID destinationId;
    private Wallet source;
    private Wallet destination;
    private TransferDtos.CreateRequest request;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        source = new Wallet(ownerId, "USD");
        destination = new Wallet(UUID.randomUUID(), "USD");
        sourceId = source.getId();
        destinationId = destination.getId();
        source.credit(new BigDecimal("25.00"));
        request = new TransferDtos.CreateRequest(sourceId, destinationId, new BigDecimal("12.50"), "USD");

        when(users.findByIdForUpdate(ownerId)).thenReturn(Optional.of(new com.example.wallet.domain.UserAccount("test@example.com", "hash")));
        when(idempotency.findByOwnerIdAndIdempotencyKey(ownerId, "key-1")).thenReturn(Optional.empty());
        when(wallets.findAllByIdInOrderByIdForUpdate(anyCollection())).thenReturn(List.of(source, destination));
    }

    @Test
    void successfulTransferDebitsAndCreditsAndWritesTwoLedgerEntries() {
        stubTransferSave();
        var response = service.create(ownerId, "key-1", request);

        assertEquals(new BigDecimal("12.50"), response.amount());
        assertEquals(new BigDecimal("12.50"), source.getBalance());
        assertEquals(new BigDecimal("12.50"), destination.getBalance());
        ArgumentCaptor<List<com.example.wallet.domain.LedgerEntry>> entries = ArgumentCaptor.forClass(List.class);
        verify(ledger).saveAll(entries.capture());
        assertEquals(2, entries.getValue().size());
        assertEquals("DEBIT", entries.getValue().get(0).getEntryType().name());
        assertEquals("CREDIT", entries.getValue().get(1).getEntryType().name());
        verify(idempotency).save(any(IdempotencyRecord.class));
        verify(outbox).save(any());
    }

    @Test
    void insufficientBalanceDoesNotWriteTransferLedgerOrIdempotency() {
        var tooMuch = new TransferDtos.CreateRequest(sourceId, destinationId, new BigDecimal("26.00"), "USD");

        ApiException error = assertThrows(ApiException.class, () -> service.create(ownerId, "key-1", tooMuch));

        assertEquals(422, error.getStatus().value());
        verify(transfers, never()).save(any());
        verify(ledger, never()).saveAll(any());
        verify(idempotency, never()).save(any());
    }

    @Test
    void sameIdempotencyKeyAndPayloadReturnsOriginalTransfer() {
        stubTransferSave();
        ArgumentCaptor<IdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        service.create(ownerId, "key-1", request);
        verify(idempotency).save(recordCaptor.capture());
        IdempotencyRecord saved = recordCaptor.getValue();
        Transfer original = new Transfer(sourceId, destinationId, request.amount(), "USD");

        when(idempotency.findByOwnerIdAndIdempotencyKey(ownerId, "key-1")).thenReturn(Optional.of(saved));
        when(transfers.findById(saved.getTransferId())).thenReturn(Optional.of(original));
        var retried = service.create(ownerId, "key-1", request);

        assertEquals(original.getId(), retried.transferId());
        verify(transfers).findById(saved.getTransferId());
        verify(wallets, org.mockito.Mockito.times(1)).findAllByIdInOrderByIdForUpdate(anyCollection());
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadIsRejected() {
        stubTransferSave();
        ArgumentCaptor<IdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);
        service.create(ownerId, "key-1", request);
        verify(idempotency).save(recordCaptor.capture());
        when(idempotency.findByOwnerIdAndIdempotencyKey(ownerId, "key-1")).thenReturn(Optional.of(recordCaptor.getValue()));

        ApiException error = assertThrows(ApiException.class, () -> service.create(ownerId, "key-1",
                new TransferDtos.CreateRequest(sourceId, destinationId, new BigDecimal("10.00"), "USD")));

        assertEquals(409, error.getStatus().value());
        verify(wallets, org.mockito.Mockito.times(1)).findAllByIdInOrderByIdForUpdate(anyCollection());
    }

    private void stubTransferSave() {
        when(transfers.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
