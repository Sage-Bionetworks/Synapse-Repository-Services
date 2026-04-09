# lib-grid-db

Relational database persistence layer for the JSON-Joy CRDT grid. Stores CRDT patches and node state in MySQL tables, enabling efficient reads without loading entire documents into memory.

## Package Structure

```
org.sagebionetworks.grid.db
├── GridDatabaseConfig       # Spring @Configuration: DataSource, JdbcTemplate, transaction manager
├── GridIndexDao / Impl      # Low-level JDBC operations against the 9 grid tables
├── GridIndexManager / Impl  # High-level patch/snapshot application with batching
├── OperationDispatcher/Impl # Routes operations to type-specific handlers
├── OperationHandler<T>      # Interface for operation-type handlers
├── GridTransaction          # Custom @Transactional annotation for grid transaction manager
├── MessageChain             # POJO for json-rx message chain tracking
├── SeekingNodeReaderProvider # Factory for snapshot file readers (mockable)
└── handler/                 # 9 OperationHandler implementations
    ├── NewConstantHandler   (new_con)
    ├── NewObjectHandler     (new_obj)
    ├── NewVectorHandler     (new_vec)
    ├── NewArrayHandler      (new_arr)
    ├── InsertValueHandler   (ins_val)
    ├── InsertObjectHandler  (ins_obj)
    ├── InsertVectorHandler  (ins_vec)
    ├── InsertArrayHandler   (ins_arr)
    └── DeleteHandler        (del)
```

## Database Tables (9 tables)

All tables are keyed by `(SESSION_ID, REPLICA_ID)` with cascading FK to `GRID_REPLICA`.

| Table | Purpose | DDL File |
|-------|---------|----------|
| `GRID_REPLICA` | Session/replica tracking, `LAST_MESSAGE_ID` | `Grid-Replica-ddl.sql` |
| `GRID_REPLICA_INDEX` | Node type index (kind ENUM: con/obj/vec/arr/val/str/bin) | `Grid-Index-ddl.sql` |
| `GRID_REPLICA_CLOCK` | Vector clock storage (replicaId → max sequence) | `Grid-Clock-ddl.sql` |
| `GRID_REPLICA_CON` | Constant nodes — JSON value + CRC32 hash for fast lookup | `Grid-Constant-ddl.sql` |
| `GRID_REPLICA_VAL` | Value nodes — JSON reference to another node | `Grid-Value-ddl.sql` |
| `GRID_REPLICA_OBJ` | Object nodes — JSON map of key→reference | `Grid-Object-ddl.sql` |
| `GRID_REPLICA_VEC` | Vector nodes — JSON array of constant references | `Grid-Vector-ddl.sql` |
| `GRID_REPLICA_RGA` | RGA array elements — predecessor links, soft-delete tombstones | `Grid-Rga-ddl.sql` |
| `GRID_REPLICA_MESSAGE` | Message chain tracking with TTL expiration | `Grid-Message-ddl.sql` |

DDL files are in `src/main/resources/schema/`.

## Architecture

### Layered Design

```
GridIndexManager        (high-level: applyPatch, applySnapshot, message chains)
    ↓
OperationDispatcher     (groups operations by type, dispatches in enum order)
    ↓
OperationHandler<T>     (one per OperationType, processes batch of typed operations)
    ↓
GridIndexDao            (raw JDBC: batch inserts, RGA traversal, clock management)
    ↓
9 MySQL tables          (via JdbcTemplate / NamedParameterJdbcTemplate)
```

### Patch Application Flow

1. `GridIndexManager.applyPatch()` checks idempotency (is patch already in replica clock?)
2. Groups patch operations by `OperationType`
3. `OperationDispatcher` routes each group to the matching `OperationHandler`
4. Each handler persists nodes via `GridIndexDao` batch methods
5. Replica clock is updated with the patch's logical timestamp
6. Returns `Map<IndexType, Set<LogicalTimestamp>>` of changed nodes

### Snapshot Import Flow

Snapshots are imported without loading the entire file into memory:
1. `SnapshotFileIndexBuilder` builds a byte-offset index of the CBOR snapshot file
2. `SeekingNodeReader` reads nodes by type range using the index
3. Nodes are batched (default 1000) and persisted via `GridIndexDao`
4. Import order: constants → objects → values → vectors (with constant lookups) → arrays + RGA elements

### RGA Insert Algorithm

The `GRID_REPLICA_RGA` table implements the Replicated Growable Array with predecessor links (`REF_REP`/`REF_SEQ`). Insertion uses a recursive CTE query (`FindInsertLocation.sql`) to find the correct position following the RGA algorithm — traversing the predecessor chain while respecting causal ordering via `(DATA_SEQ, DATA_REP)` comparison.

`ListArrayOrder.sql` provides paginated traversal of RGA arrays using a recursive CTE with `LIMIT`/`OFFSET`.

## Key Patterns

### `@GridTransaction` Annotation

Custom transaction annotation that binds to `gridTransactionManager` (NOT the main app's primary transaction manager). Required because the grid uses a **separate database** from the main Synapse database. Always uses `READ_COMMITTED` isolation and `REQUIRED` propagation.

### Handler Pattern

Each `OperationHandler<T>` is a Spring `@Component` discovered via constructor injection into `OperationDispatcherImpl`. Handlers are stored in a map keyed by `OperationType` for O(1) dispatch. All handlers follow the same structure: extract IDs, save to index, persist type-specific data, return changed timestamps.

### Idempotency

Patches are deduplicated via the replica clock. If a patch's sequence number is already present in `GRID_REPLICA_CLOCK`, the patch is skipped. This makes reapplication safe.

### Soft Deletes

RGA nodes use `IS_DELETED` boolean flag (tombstone) rather than physical deletion, preserving causal ordering for concurrent operations.

### Message Chains

Track json-rx request/response chains with TTL-based expiration. Message IDs cycle from 0 to 65535 per the json-rx spec. Used for coordinating WebSocket message sequences.

## Spring Configuration

- `grid-db-spb.xml` — component scan for `org.sagebionetwork grid`, imports `stack-configuration.spb.xml`
- `GridDatabaseConfig` — `@Configuration` class creating `BasicDataSource`, `JdbcTemplate`, `DataSourceTransactionManager` beans

## Testing

- `GridIndexDaoImplTest` — Spring integration test (`@ContextConfiguration`) testing all DAO operations
- `GridIndexManagerImplTest` — Mockito unit test for patch/snapshot logic
- `GridIndexManagerAutowiredTest` — Performance/scale test (10M cells target, PLFM-9032)
- `OperationDispatcherImplTest` — Tests operation grouping and handler dispatch
- One unit test per handler (e.g., `NewConstantHandlerTest`, `InsertArrayHandlerTest`)
