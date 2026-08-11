# lib/stackConfiguration

Environment/stack configuration and AWS client construction. Every stack is identified by `stack` (dev/prod) + `instance`; this module resolves all config properties and builds AWS clients from them.

## Accessing configuration

- Spring code injects the `StackConfiguration` bean normally.
- Code **outside** the Spring context reads config via `StackConfigurationSingleton.singleton()`.

**`StackConfigurationSingleton` bootstraps with Guice, not Spring** (`Guice.createInjector(new StackConfigurationGuiceModule())`). This is the one place in the codebase using Guice DI — it exists so non-Spring code paths can still get a fully-wired `StackConfiguration`. Do NOT "fix" it to Spring DI; the whole point is to avoid requiring a Spring context here.

## Queue name resolution

`stackConfig.getQueueName("BASE_NAME")` returns `{stack}-{instance}-BASE_NAME`. Workers use this to resolve SQS queue URLs at runtime; the physical queues are provisioned by the separate Synapse-Stack-Builder project.

## AWS client construction — v1/v2 migration in progress

Two factories coexist while the codebase migrates from AWS SDK v1 to v2:

- `aws/AwsClientFactory` — SDK **v1** clients (legacy).
- `aws/v2/AwsClientFactoryV2` — SDK **v2** clients.
- `aws/ProfileCredentialsProviderV2V1Adapter` — bridges the two credential-provider hierarchies so a single resolved credential source feeds both.

**New AWS clients should use `AwsClientFactoryV2` (v2).** When a subsystem still needs a v1 client, reuse the adapter rather than constructing a parallel credential chain.

## Constraints

- **No secrets in code or config committed here** — property *names* only; values come from the deployed stack.
