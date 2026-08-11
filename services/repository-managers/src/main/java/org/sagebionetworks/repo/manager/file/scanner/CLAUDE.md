# manager/file/scanner

Scans DBO tables for file-handle references so orphaned/associated file handles can be reconciled. The non-obvious part: the scanner generates its SQL automatically from DBO metadata rather than hand-written queries.

## Metadata-driven SQL

`BasicFileHandleAssociationScanner` builds min/max range queries and batched `SELECT`s directly off a `TableMapping`'s `FieldColumn[]`:

- It locates the column with `FieldColumn.hasFileHandleRef() == true` (the file-handle column) and the `FieldColumn.isBackupId() == true` column (the range/scan key).
- **It throws at construction if either column is missing** — a table with no file-handle-ref column, or no backup-id column, cannot be scanned. So a new scannable DBO must set `withHasFileHandleRef(true)` and have a backup-id column in its `FieldColumn` definitions.
- Row mapping is pluggable via `RowMapperSupplier` (e.g. `SerializedFieldRowMapperSupplier` for file handles stored inside a serialized blob column).

## Adding a scanner for a new table

Point `BasicFileHandleAssociationScanner` at the DBO's `TableMapping` and supply a `RowMapperSupplier`. Do not hand-write range/scan SQL — the generated SQL keeps the scan batched over the backup-id range consistently across all association types.
