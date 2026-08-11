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
- **Lists of objects as a payload**: When a method needs to accept (or return) a *list* — a controller/API request or response body, or a tool argument — do NOT use a bare `List<T>` / top-level array. Define a new request/response schema with a **single property that is an array**, and pass that POJO instead. This is the standard Synapse shape: it keeps the payload a JSON object (extensible with sibling fields later) and generates a named, documented type. Example: to accept a list of IDs, define a schema `{"properties": {"entityIds": {"type": "array", "items": {"type": "string"}}}}` and take that request object as the argument.
- **Required fields**: `"required": true` on a property
- **Map types**: For map-like properties, either define the sub-type schema explicitly OR treat the entire value as a plain `"type": "string"` (expecting a JSON string). Do NOT use `Map<String, String>` where the string values are themselves serialized JSON — this creates ambiguous "JSON within JSON" that bypasses schema validation.
- **Descriptions must match implementation**: Schema descriptions (especially for optional fields like "If null, lists X") are API contracts. Always verify the implementation matches the schema description. If behavior changes, update the schema text to match.
- **Mirroring an external API's shape**: When a schema family models a pass-through external API (e.g. the `search/dsl/` package — 60+ schemas mirroring the OpenSearch query DSL, `$ref`-composed), keep field names and nesting **identical to that external spec** rather than Synapse-idiomatic naming, so the objects serialize straight through to the external service.

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
