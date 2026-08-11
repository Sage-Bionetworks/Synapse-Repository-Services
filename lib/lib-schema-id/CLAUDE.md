# lib/lib-schema-id

Parser and model for JSON Schema `$id` strings. A Synapse schema `$id` is `organizationName-schemaName[-semanticVersion]` (semver optional), and this module parses/represents/re-renders that grammar.

## Grammar as a composite tree

The `$id` is modeled as a tree of `org.sagebionetworks.schema.element.Element` nodes, each of which renders itself into a shared buffer via `abstract void toString(StringBuilder)` — a visitor-style composition, not a plain `Object.toString()`. Build the string by creating a `StringBuilder` and calling the root's `toString(builder)`.

- `SchemaId` (root) = `OrganizationName` + `DASH` + `SchemaName` + optional (`DASH` + `SemanticVersion`).
- `SimpleBranch` wraps a single child and delegates (`final`); leaves like `SimpleString`/`AlphanumericIdentifier` append their own text.
- `SemanticVersion` decomposes into `VersionCore` / `Prerelease` / `Build` sub-elements.

## Conventions

- **The join separator is `SchemaId.DASH` (`"-"`)** — reuse the constant; don't hardcode delimiters when composing or splitting.
- **Compose via `toString(StringBuilder)`, never string concatenation** — the whole point of the `Element` hierarchy is that each node knows how to render itself, so new grammar pieces must implement `toString(StringBuilder)` rather than overriding a bare `toString()`.
- Elements reject null children in their constructors (`IllegalArgumentException`) — construct fully-formed subtrees.
