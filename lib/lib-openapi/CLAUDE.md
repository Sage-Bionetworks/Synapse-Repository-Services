# lib/lib-openapi

Build-time **javadoc doclet** that reflects over the annotated REST controllers and emits the OpenAPI specification JSON. It is not run by tests — it runs via the `maven-javadoc-plugin` in `services/repository/pom.xml`. Package: `org.sagebionetworks.translator`.

## How it runs

`ControllerModelDoclet` is invoked as a doclet from `services/repository/pom.xml` with `additionalparam`:
`--target-file,<...>/openapispecification.json,--factory-path,org.sagebionetworks.server.ServerSideOnlyFactory,--should-run,true`.

- **`--should-run,true` is mandatory.** If it's anything but `true`, the doclet silently no-ops (prints a NOTE and produces no spec). This is deliberate so a normal javadoc build doesn't try to generate the spec — but it means "no spec was generated" is usually a missing/false `--should-run`, not a code bug.

## Conventions that affect controller authors

`ControllerToControllerModelTranslator` turns controller methods into the model. When adding or changing a controller method, respect what the translator enforces (it throws at build time otherwise):

- A controller class **must** carry the `ControllerInfo` annotation, and each translated method **must** have a `RequestMapping`.
- **`@Deprecated` methods are skipped** — they do not appear in the generated spec.
- **Each parameter must have exactly one relevant annotation** — `annotations.size() != 1` throws. A parameter with zero or multiple binding annotations breaks the build.
- Parameter types the API doesn't expose (e.g. `HttpServletResponse`, the user-id param) must be in the `PARAMETERS_NOT_REQUIRED_TO_BE_TRANSLATED` allowlist, or translation fails on them.
- Generic wrapper types are mapped via the `CUSTOM_GENERIC_CLASS_TO_GENERIC_PROPERTY` map; a new generic container type used in a response may need an entry there.

## Related

A sibling doclet framework, `lib/lib-javadoc` (`SpringMVCDoclet`), generates the HTML API docs from the same controllers via Velocity templates — separate output, separate pipeline.
