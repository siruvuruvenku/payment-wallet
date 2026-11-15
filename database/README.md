# Database schema and queries

The application database is MySQL 8. The Spring Boot application creates/upgrades it through the versioned Flyway migrations in [backend/src/main/resources/db/migration](../backend/src/main/resources/db/migration).

## Create the database and all tables manually

From PowerShell at the project root, run the complete standalone DDL script:

```powershell
Get-Content .\database\schema.sql | mysql -u root -p
```

Or, from the MySQL command line:

```sql
SOURCE database/schema.sql;
```

This creates `wallet` and all eight tables: `app_users`, `wallets`, `transfers`, `ledger_entries`, `idempotency_records`, `outbox_events`, `audit_logs`, and `reconciliation_records`. The script is for a new database; Flyway is the recommended path for an existing application database. Do not manually apply this script over an already-migrated database.

To run the example read/reconciliation queries after selecting the database:

```powershell
Get-Content .\database\queries.sql | mysql -u root -p wallet
```

Replace the example UUID values in `queries.sql` with real values from your database. They are placeholders, not actual accounts.

## Schema and consistency notes

The schema uses UUID primary keys stored as `BINARY(16)`, `DECIMAL(19,2)` for balances and transfer amounts, foreign keys, positive-amount/nonnegative-balance check constraints, and indexes for ownership and history queries. Wallet rows include an optimistic-lock `version`; the transfer service additionally takes pessimistic row locks.

Each completed transfer should have exactly one debit and one credit. The unique `(transfer_id, entry_type)` constraint prevents duplicate entry types; application transaction logic creates both entries atomically. Use the ledger consistency query in `queries.sql` to detect missing or mismatched pairs. Audit and reconciliation tables exist in the schema, but application code does not yet write audit records or run reconciliation jobs.
