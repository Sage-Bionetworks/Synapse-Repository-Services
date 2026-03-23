# lib/lib-utils

General-purpose utility classes shared across the codebase. Compiled to **Java 8** for SWC (Synapse Web Client) compatibility.

## Key Classes

### ValidateArgument

The standard input validation utility — used in every manager and DAO:

```java
ValidateArgument.required(userInfo, "userInfo");           // non-null check
ValidateArgument.requiredNotBlank(name, "name");           // non-null, non-empty, non-whitespace
ValidateArgument.requiredNotEmpty(list, "list");            // non-null, non-empty collection
ValidateArgument.requirement(count > 0, "count must be positive");  // boolean condition
ValidateArgument.validUrl(url, "url");                     // URL format validation
```

All methods throw `IllegalArgumentException` on failure.

### Other Utilities

| Class | Purpose |
|-------|---------|
| `Pair<F, S>` | Simple immutable pair |
| `TimeUtils` | Time-related helpers |
| `Clock` / `DefaultClock` | Abstraction for `System.currentTimeMillis()` (testable) |
| `ProgressCallback` / `ProgressListener` | Progress reporting interface used by workers |
| `RetryException` | Exception for retry-eligible failures |

## Build

```
mvn clean install -pl lib/lib-utils -DskipTests
```
