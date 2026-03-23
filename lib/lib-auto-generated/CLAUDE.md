# lib/lib-auto-generated

JSON Schema definitions and auto-generated POJOs for the Synapse API model. **Do NOT edit generated classes** — edit the JSON schema, then rebuild.

## How It Works

1. JSON schemas live in `src/main/resources/schema/org/sagebionetworks/`
2. The `schema-to-pojo-maven-plugin` generates Java classes during the build
3. Generated POJOs go to `target/auto-generated-pojos/`
4. Both schemas and generated sources are packaged in the JAR

## Schema Directory Structure

Schemas mirror Java package paths:
```
src/main/resources/schema/org/sagebionetworks/
├── repo/model/                  # Core entity models (Entity, FileEntity, Folder, Project)
│   ├── table/                   # Table-related types (TableEntity, ColumnModel)
│   ├── doi/                     # DOI types
│   ├── auth/                    # Authentication types
│   ├── schema/                  # JSON Schema validation types
│   ├── grid/                    # Grid/Curator types
│   └── ...                      # Many more domain sub-packages
└── ...
```

## Schema Format

### Concrete Class

```json
{
    "title": "File Entity",
    "description": "A file in Synapse.",
    "implements": [
        { "$ref": "org.sagebionetworks.repo.model.VersionableEntity" }
    ],
    "properties": {
        "dataFileHandleId": {
            "type": "string",
            "description": "The ID of the file handle."
        }
    }
}
```

### Interface

```json
{
    "description": "Base interface for all entities.",
    "type": "interface",
    "properties": {
        "name": { "type": "string", "description": "The name of this entity." },
        "id": { "type": "string", "description": "The unique ID.", "transient": true }
    }
}
```

### Enum

```json
{
    "description": "The type of access.",
    "name": "ACCESS_TYPE",
    "type": "string",
    "enum": [
        { "name": "READ", "description": "Read access." },
        { "name": "UPDATE", "description": "Update access." }
    ]
}
```

## Key Schema Conventions

- **References**: Use fully qualified package path: `"$ref": "org.sagebionetworks.repo.model.Entity"`
- **`type: "interface"`**: Generates a Java interface instead of a class
- **`implements`**: Array of `$ref` entries for interface inheritance
- **`transient: true`**: Field exists in POJO but is NOT serialized to JSON (used for id, etag, createdOn)
- **`format: "date-time"`**: String property treated as ISO 8601 timestamp
- **Arrays**: `"type": "array"` with `"items": { "$ref": "..." }`, optional `"uniqueItems": true`
- **Required fields**: `"required": true` on a property

## Generated Code Patterns

- Concrete classes implement `JSONEntity` and `Serializable`
- Fluent setters (return `this` for chaining)
- Auto-generated `hashCode()`, `equals()`, `toString()`
- Auto-generated JSON serialization/deserialization via `JSONObjectAdapter`
- Static `_KEY_*` constants for JSON field names
- Factory class: `ServerSideOnlyFactory` (server-side object instantiation)

## Adding a New Model Object

1. Create a `.json` schema file in the appropriate sub-package under `src/main/resources/schema/org/sagebionetworks/`
2. Define properties, types, and any interface implementations
3. Run `mvn clean install -pl lib/lib-auto-generated -DskipTests` to generate the POJO
4. The generated class appears in `target/auto-generated-pojos/` — do NOT copy or edit it
5. Reference the new type from other schemas via `$ref` or use it directly in manager/DAO code

## Build

```
mvn clean install -pl lib/lib-auto-generated -DskipTests   # Regenerate POJOs
```

Note: Compiled to **Java 8** (not 11) for GWT client compatibility.
