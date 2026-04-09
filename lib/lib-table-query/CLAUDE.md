# lib/lib-table-query

SQL query parser for Synapse table queries, built with **JavaCC** (Java Compiler Compiler). Parses user-supplied SQL into an AST (Abstract Syntax Tree) used by `lib-table-cluster` and `repository-managers` for table/view operations.

## Code Generation

- **Grammar source**: `src/main/resources/` — JavaCC `.jj` grammar files define the SQL subset
- **Generated parser**: `target/generated-sources/javacc/` — JavaCC generates the parser, token manager, and AST node classes during the build
- **Classpath integration**: `build-helper-maven-plugin` adds generated sources to the classpath automatically

**Do NOT edit files in `target/generated-sources/`** — modify the grammar in `src/main/resources/` and rebuild.

## Build

```
mvn clean install -pl lib/lib-table-query -DskipTests    # Regenerate parser
mvn test -pl lib/lib-table-query                          # Run parser tests
```

After building, the generated parser classes appear in `target/generated-sources/javacc/`.

## Modifying the Grammar

1. Edit the `.jj` grammar file in `src/main/resources/`
2. Rebuild: `mvn clean install -pl lib/lib-table-query -DskipTests`
3. Verify generated output in `target/generated-sources/javacc/`
4. Run tests: `mvn test -pl lib/lib-table-query`
5. Update downstream consumers in `lib-table-cluster` and `repository-managers` if the AST changes

## Dependencies

- `lib-models` — model interfaces used by the AST

## Code Coverage

Minimum thresholds enforced by JaCoCo:
- Branch coverage: 70%
- Line coverage: 80%
