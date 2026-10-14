# service/metadata

The **entity provider** plugin architecture — type-specific hooks that run during entity create/update/delete/version operations. This is how per-`EntityType` behavior is attached without a central switch.

## Extension interfaces

An entity provider implements one or more marker interfaces (all extend `EntityProvider<T>`), each invoked at a specific point:

- `EntityValidator` — validate on create/update
- `TypeSpecificCreateProvider` / `TypeSpecificUpdateProvider` / `TypeSpecificDeleteProvider` / `TypeSpecificVersionDeleteProvider` — lifecycle hooks
- `TypeSpecificMetadataProvider` — post-read metadata population
- `DefiningSqlProvider` — for defining-SQL entity types

## Registry + naming convention

`MetadataProviderFactoryImpl` holds a `Map<EntityType, EntityProvider<?>>` populated by an `@Autowired` constructor/setter that takes each concrete provider bean explicitly, keyed by `EntityType`.

**Adding a new entity type:** create a `@Service` provider named `{DTO}MetadataProvider` (e.g. `DatasetMetadataProvider`, `RecordSetMetadataProvider`) implementing the hook interfaces you need, and wire it into `MetadataProviderFactoryImpl`'s provider map. **If you skip the wiring or misname the class, the entity type is silently unwired** — its validators and lifecycle hooks never run, with no startup error. This is the most common mistake when introducing a new entity type.

## Notes

- `AllTypesValidator` runs for every entity regardless of type (name/hierarchy rules), separate from the type-specific `EntityValidator`s.
