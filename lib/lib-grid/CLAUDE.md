# lib-grid

Model objects for the JSON-Joy CRDT (Conflict-free Replicated Data Type) grid used by the Curation Grid feature. This module defines the data structures for nodes, patches, operations, and CBOR encoding — it has **no database or Spring dependencies**.

## Package Structure

```
org.sagebionetworks.repo.model.grid
├── node/         # CRDT node types (the document's building blocks)
├── patch/        # Patch and LogicalTimestamp (change units)
│   └── operation/          # Typed operations within a patch
│       └── builder/        # Fluent builders for creating operations
├── encoding/     # CBOR serialization, snapshot file I/O, seeking readers
└── ClockTable    # Utility for vector clock management
```

## Key Concepts

### Logical Timestamps

Every node and operation is identified by a `LogicalTimestamp` (replicaId + sequenceNumber). Timestamps are comparable and totally ordered (sequence first, then replicaId). Compact string form: `"replicaId.sequenceNumber"`.

### Node Types (`IndexType` enum)

| Type  | Class          | Description |
|-------|----------------|-------------|
| `con` | `ConstantNode` | Immutable value (JSON/CBOR). Holds a `ConValue` with a `ConType` discriminator. |
| `val` | `ValueNode`    | Mutable reference to another node (LWW register). |
| `obj` | `ObjectNode`   | Map of string keys to node references (JSON object). |
| `vec` | `VectorNode`   | Append-only indexed array of constant references (max 256). Used for column names and row cell data. |
| `arr` | `RGANode`/`ArrayNode` | Replicated Growable Array — ordered list with insert/delete. Used for column order and row order. Nodes link via `refId` (predecessor). Deletes are soft (tombstone `isDeleted` flag). |
| `str` | *(reserved)*   | String CRDT (not yet used by grid). |
| `bin` | *(reserved)*   | Binary CRDT (not yet used by grid). |

All node classes implement the `Node` interface (`getId()`, `streamReferencedTimestamps()`).

### Patches and Operations

A `Patch` is an atomic unit of change with a `patchId` (LogicalTimestamp) and a list of `Operation` objects. Each operation consumes a `span` of logical clock cycles.

**`OperationType` enum** (with numeric codes matching json-joy compact format):

| Code | Type      | Description |
|------|-----------|-------------|
| 0    | `new_con` | Create new constant node |
| 1    | `new_val` | Create new value node |
| 2    | `new_obj` | Create new object node |
| 3    | `new_vec` | Create new vector node |
| 6    | `new_arr` | Create new RGA array node |
| 9    | `ins_val` | Update a value node's reference |
| 10   | `ins_obj` | Insert a property into an object |
| 11   | `ins_vec` | Insert an element into a vector |
| 14   | `ins_arr` | Insert element(s) into an RGA array |
| 16   | `del`     | Mark RGA node(s) as deleted (tombstone) |
| 17   | `nop`     | No-op |

### CBOR Encoding (`encoding/` package)

- Patches and snapshots use **CBOR** (Concise Binary Object Representation) via Jackson `jackson-dataformat-cbor`
- `SnapshotFileIndexBuilder` — builds a byte-offset index of a CBOR snapshot file, enabling random access by node type
- `SeekingNodeReader` — reads specific node ranges from a snapshot file without loading the entire file into memory
- `IndexedNodeCodecMapper` — maps between CBOR-encoded nodes and Java model objects

## Dependencies

- `lib-utils` — `ValidateArgument` and general utilities
- `org.json:JSON-Java` — JSON manipulation for `ConValue` compact format
- `jackson-dataformat-cbor` — CBOR serialization

## Spec References

- [JSON-Joy CRDT Patch](https://jsonjoy.com/specs/json-crdt-patch/patch-document/patch-structure)
- [JSON-Joy Compact Encoding](https://jsonjoy.com/specs/json-crdt-patch/encoding/compact-format)
- [json-rx Messages](https://jsonjoy.com/specs/json-rx/messages)
