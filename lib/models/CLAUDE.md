# lib/models

DAO interfaces and shared exception types. This module defines the persistence contracts that `lib/jdomodels` implements. Compiled to **Java 8** for SWC compatibility.

## Package Structure

```
org.sagebionetworks.repo.model
├── dao/                    # DAO interfaces (NodeDAO, AccessControlListDAO, etc.)
├── (root)                  # Core interfaces, enums, utility classes
└── ...                     # Domain-specific sub-packages
```

## DAO Interfaces

Interfaces define persistence contracts without implementation details:

```java
public interface NodeDAO {
    Node createNewNode(Node node);
    Node getNode(String id);
    void delete(String id);
    // ...
}
```

Implementations live in `lib/jdomodels/` under `org.sagebionetworks.repo.model.dbo.dao`.

## Exception Types

Standard exceptions used across the codebase:

| Exception | HTTP Status | Usage |
|-----------|-------------|-------|
| `DatastoreException` | 500 | General persistence errors |
| `NotFoundException` | 404 | Entity/resource not found |
| `UnauthorizedException` | 401/403 | Access denied |
| `ConflictingUpdateException` | 409 | Optimistic locking conflict (stale etag) |
| `InvalidModelException` | 400 | Invalid entity state |
| `TooManyRequestsException` | 429 | Rate limiting |

## Build

```
mvn clean install -pl lib/models -DskipTests
```
