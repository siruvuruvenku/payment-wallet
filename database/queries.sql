USE wallet;

-- MySQL stores application UUID values as 16-byte binary values.
-- Replace each example UUID with a real UUID when running these queries.

-- Find the wallets owned by a user.
SELECT BIN_TO_UUID(w.id) AS wallet_id,
       w.currency,
       w.balance,
       w.status,
       w.created_at
FROM wallets w
WHERE w.owner_id = UUID_TO_BIN('11111111-1111-1111-1111-111111111111')
ORDER BY w.created_at DESC;

-- Read one wallet balance. The API additionally verifies that the caller owns it.
SELECT BIN_TO_UUID(id) AS wallet_id,
       currency,
       balance AS available_balance,
       status
FROM wallets
WHERE id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222');

-- Read paginated transfers sent from or received by one wallet.
SELECT BIN_TO_UUID(t.id) AS transfer_id,
       BIN_TO_UUID(t.source_wallet_id) AS source_wallet_id,
       BIN_TO_UUID(t.destination_wallet_id) AS destination_wallet_id,
       t.amount,
       t.currency,
       t.status,
       t.created_at
FROM transfers t
WHERE t.source_wallet_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
   OR t.destination_wallet_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
ORDER BY t.created_at DESC
LIMIT 20 OFFSET 0;

-- Read a wallet's immutable ledger history.
SELECT BIN_TO_UUID(le.id) AS entry_id,
       BIN_TO_UUID(le.transfer_id) AS transfer_id,
       le.entry_type,
       le.amount,
       le.currency,
       le.created_at
FROM ledger_entries le
WHERE le.wallet_id = UUID_TO_BIN('22222222-2222-2222-2222-222222222222')
ORDER BY le.created_at DESC
LIMIT 20 OFFSET 0;

-- Find completed transfers that do not have exactly one matching debit and credit.
-- A result row indicates an incomplete or inconsistent ledger pair.
SELECT t.id AS transfer_id,
       t.source_wallet_id,
       t.destination_wallet_id,
       t.amount AS transfer_amount,
       COALESCE(SUM(le.entry_type = 'DEBIT'), 0) AS debit_count,
       COALESCE(SUM(le.entry_type = 'CREDIT'), 0) AS credit_count,
       COALESCE(SUM(le.entry_type = 'DEBIT' AND le.wallet_id = t.source_wallet_id
           AND le.amount = t.amount AND le.currency = t.currency), 0) AS matching_debit_count,
       COALESCE(SUM(le.entry_type = 'CREDIT' AND le.wallet_id = t.destination_wallet_id
           AND le.amount = t.amount AND le.currency = t.currency), 0) AS matching_credit_count
FROM transfers t
LEFT JOIN ledger_entries le ON le.transfer_id = t.id
WHERE t.status = 'COMPLETED'
GROUP BY t.id, t.source_wallet_id, t.destination_wallet_id, t.amount, t.currency
HAVING debit_count <> 1
    OR credit_count <> 1
    OR matching_debit_count <> 1
    OR matching_credit_count <> 1;

-- Inspect events that still need an outbox publisher to deliver them.
SELECT BIN_TO_UUID(id) AS event_id,
       BIN_TO_UUID(aggregate_id) AS aggregate_id,
       event_type,
       payload,
       created_at
FROM outbox_events
WHERE published_at IS NULL
ORDER BY created_at
LIMIT 100;

-- Inspect reconciliation findings for a single run.
SELECT BIN_TO_UUID(run_id) AS run_id,
       BIN_TO_UUID(transfer_id) AS transfer_id,
       check_type,
       status,
       expected_debit,
       actual_debit,
       expected_credit,
       actual_credit,
       details,
       created_at
FROM reconciliation_records
WHERE run_id = UUID_TO_BIN('33333333-3333-3333-3333-333333333333')
ORDER BY created_at DESC;
