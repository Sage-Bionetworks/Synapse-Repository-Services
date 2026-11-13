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

### S3 object access — `aws/v2/S3ObjectStore`

S3 has a narrowed facade rather than a client-shaped one. `S3ObjectStore` (impl
`S3ObjectStoreImpl`, built by `AwsClientFactoryV2.createS3ObjectStore()`) covers only object
access: get/put/delete, object info, object tags, pre-signed downloads, and the two
bucket-location questions callers actually ask (`verifyBucketAccess`, `isSameRegion`).

- **No SDK type appears in its signatures.** Values cross the seam as `S3ObjectInfo`,
  `S3WriteOptions`, `S3ObjectTag`, `S3ResponseHeaders`, `S3StorageClass`, `S3CannedAcl` — all in
  the same package, all carrying only what Synapse uses. Extend those types rather than exposing
  an SDK enum or model class through the facade.
- **Everything else uses a raw `S3Client` at its consumer** — multipart, bucket administration,
  CORS, `restoreObject`, listing. A consumer holding both the facade and a raw `S3Client` is the
  intended shape, not a smell.
- **Bucket failures become `org.sagebionetworks.aws.CannotDetermineBucketLocationException`**,
  which several consumers catch for control flow. The translation predicate is
  `S3ObjectStoreImpl.isBucketAccessFailure`; every other `S3Exception` propagates.
- **Region resolution lives here, only because pre-signing needs it.** The client is
  `crossRegionAccessEnabled`, so it finds a bucket's region itself; a `S3Presigner` cannot, since
  signing is local computation. The facade resolves bucket regions with a cached `headBucket`
  probe and builds one presigner per region.

This module compiles with `<release>8</release>` for SWC compatibility, so **no Java 9+ language
or library features here** — the value types above are immutable classes with builders, not
records.

## Constraints

- **No secrets in code or config committed here** — property *names* only; values come from the deployed stack.
