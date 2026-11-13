# Contributing to Synapse-Repository-Services

## CI re-runs

A GitHub push runs `build_and_test.yml`, which builds and tests the branch on your CodePipeline (`Synapse-Build-<user>`).
When you re-run that same workflow run, the new build leaves out the test classes that already passed in an earlier build of the run, and runs everything else.

### What counts as a re-run

These reuse the run's earlier results:

- "Re-run all jobs" and "Re-run failed jobs" on the GitHub workflow run.
- "Retry stage" on the `BuildSynapseRepo` stage in the CodePipeline console.

These run the full suite:

- The first attempt of every workflow run, so a new push always runs everything.
- Local builds and Jenkins builds: the feature is off without the `rerun-identity.txt` file that the workflow puts in the source zip.
- A re-run whose build inputs changed since the earlier builds: the Stack Builder commit, `FULL_BUILD` or `EXTRA_ARGS`.

To force a full suite on a run, push a new commit or change `EXTRA_ARGS`.

### What is left out

A class is left out only if it passed in an earlier build of the run with the same build inputs, and that build saved coverage data for the class's module (or the module skips JaCoCo).
Everything else runs, including:

- Classes that failed in an earlier build.
- Classes in modules that never ran, such as the modules after the one that failed (`mvn install` stops at the first failing module).
- Classes whose tests are all disabled.

Each module's coverage minimum is still checked, against this build's coverage data merged with that of the earlier builds.
A `-Dtest=...` in `EXTRA_ARGS` overrides the left-out list.

### Reading the build log

Before Maven starts, the log shows a `re-run records:` banner with the earlier builds it used, how many classes it left out in each module, and why it ran the full suite or ignored an earlier build.
After Maven, a second `re-run records:` block shows per-module counts, the classes that failed earlier and passed now ("rescued"), and the first-time failures.
The full class lists are in `summary.txt` next to the build's record, under `s3://synapse-build-source-<user>/reruns/<run id>/<CodeBuild build id>/`.

A problem reading or writing the records never fails the build; at worst the build runs the full suite.

### Limits

- If a test JVM dies while JaCoCo writes its coverage file, the cut-off file makes the coverage merge fail on every re-run of that run. Push a new commit to start a new run.
- "Re-run failed jobs" does not refresh the AWS session token that the build uses. More than about 8 hours after the latest `build_and_test` job on your fork, use "Re-run all jobs".

### Changing the feature

The logic is in `configuration/build/rerun_records.py` (`prepare` before Maven, `record` after it, both called from `configuration/build/buildspec.yml`) and the root pom's `rerun-coverage` profile.
Run `python3 configuration/build/rerun_records.py selftest` after a change; a pre-commit hook runs it too.
A new environment variable passed to the `BuildSynapseRepo` build must be added to the fingerprint or to the script's list of variables that cannot change which tests pass, or the selftest fails.
