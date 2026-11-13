#!/usr/bin/env python3
"""Skip test classes that already passed when a GitHub workflow run is re-run.

Every CodeBuild build of the Synapse repository belongs to one GitHub workflow run.
When a developer re-runs that workflow run, this script lets the new build leave out
the test classes that already passed in an earlier build of the same run, and run
everything else. See "CI re-runs" in CONTRIBUTING.md for the developer-facing view.

Commands:
    prepare: Runs before Maven. Reads the earlier builds' records from S3, writes each
        module's `coverage-data/excludes.txt` for Surefire and Failsafe, restores the
        earlier builds' JaCoCo data into `coverage-data/`, and writes the Maven flag
        that turns on the root pom's `rerun-coverage` profile.
    record: Runs after Maven, also after a failed build. Saves this build's passed and
        failed classes, its JaCoCo data and a fingerprint of its inputs to S3.
    selftest: Checks the selection and recording rules against local fixtures,
        without AWS. Run by a pre-commit hook.

A build's record lives under `s3://<bucket>/reruns/<run id>/<CodeBuild build id>/`:

    fingerprint.txt: The build inputs that can differ between builds of one run.
    passed.tsv: One line per passed top-level class: module dir, class name.
    failed.tsv: One line per failed top-level class, in the same format.
    no-coverage.txt: Modules whose tests ran without a JaCoCo agent (jacoco.skip).
    coverage/<module>.exec: The module's JaCoCo data from this build, if not empty.
    summary.txt: Per-module counts and full class lists, for people.
    complete: The time the record was finished; uploaded last.

Any problem with the records makes the build run more tests, never fewer, and
neither `prepare` nor `record` changes the build result: both always exit 0.
"""

from __future__ import annotations

import contextlib
import dataclasses
import datetime
import io
import json
import os
import re
import shutil
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Callable

# Files at the source root. The workflow writes the identity file into the source zip.
IDENTITY_FILE = "rerun-identity.txt"
ARGS_FILE = "rerun-maven-args.txt"
STATE_FILE = "rerun-state.json"

# Per-module folder, kept outside target/ so that `mvn clean` leaves it alone. The
# root pom's rerun-coverage profile reads excludes.txt and writes current.exec here.
COVERAGE_DIR = "coverage-data"
CURRENT_EXEC = "current.exec"
MAVEN_FLAG = "-Dsynapse.rerun"

# Surefire drops its default inner-class exclude once an excludes file is given.
INNER_CLASS_EXCLUDE = "**/*$*"
REPORT_DIRS = ("surefire-reports", "failsafe-reports")
NOT_SOURCE_DIRS = {".git", ".m2", "target", "node_modules", COVERAGE_DIR}
RECORD_FILES = (
    "complete",
    "fingerprint.txt",
    "passed.tsv",
    "failed.tsv",
    "no-coverage.txt",
)

# Every environment variable that the pipeline passes to the BuildSynapseRepo build is
# either part of the fingerprint or cannot change which tests pass. The self-check
# fails when the CodePipeline template adds a variable that is in neither.
FINGERPRINT_ENV = {"FULL_BUILD": "full_build", "EXTRA_ARGS": "extra_args"}
NOT_FINGERPRINTED_ENV = {
    "user",  # selects the pipeline and its bucket, so records never cross users
    "ARTIFACT_VERSION",  # a timestamp that changes on every develop re-run
}
FINGERPRINT_FIELDS = ("stack_builder_key", "full_build", "extra_args")

# A test class as (module dir, fully qualified top-level class name).
ClassRef = tuple[str, str]
Downloader = Callable[[str, str, Path], None]
Uploader = Callable[[str, str, Path], None]


@dataclasses.dataclass
class BuildRecord:
    """One earlier build's record, read from a local copy of its S3 folder.

    Attributes:
        build_id: The CodeBuild build id, which names the record's folder.
        folder: The local copy of the record's folder.
        completed: When the build finished its record; orders the builds.
        fingerprint: The build's inputs, by FINGERPRINT_FIELDS name.
        passed: The classes that passed in the build.
        failed: The classes that failed in the build.
        no_coverage: The modules whose tests ran without a JaCoCo agent.
    """

    build_id: str
    folder: Path
    completed: str
    fingerprint: dict[str, str]
    passed: set[ClassRef]
    failed: set[ClassRef]
    no_coverage: set[str]

    def coverage_file(self, module: str) -> Path:
        """Returns where this build's JaCoCo data for the module would be."""
        return self.folder / "coverage" / f"{module_key(module)}.exec"


def download_records(bucket: str, prefix: str, dest: Path) -> None:
    """Downloads every S3 object under a prefix into a local folder.

    An object that cannot be downloaded is left out, so the selection treats it the
    same as an object that was never uploaded.

    Arguments:
        bucket: The S3 bucket.
        prefix: The key prefix, ending in "/"; the part after it becomes the path.
        dest: The local folder to download into.
    """
    import boto3  # only on the CodeBuild host; the self-check runs without it

    s3 = boto3.client("s3")
    pages = s3.get_paginator("list_objects_v2").paginate(Bucket=bucket, Prefix=prefix)
    for page in pages:
        for item in page.get("Contents", []):
            target = dest / item["Key"][len(prefix) :]
            target.parent.mkdir(parents=True, exist_ok=True)
            try:
                s3.download_file(bucket, item["Key"], str(target))
            except Exception as e:
                print(f"re-run records: cannot download {item['Key']} ({e!r})")
                target.unlink(missing_ok=True)


def upload_records(bucket: str, prefix: str, folder: Path) -> None:
    """Uploads a local folder under an S3 prefix, with the `complete` file last.

    A later build uses a record only if its `complete` file exists, so a record that
    fails partway through the upload is never used.

    Arguments:
        bucket: The S3 bucket.
        prefix: The key prefix, ending in "/".
        folder: The local folder to upload; it must contain a `complete` file.
    """
    import boto3  # only on the CodeBuild host; the self-check runs without it

    s3 = boto3.client("s3")
    files = sorted(p for p in folder.rglob("*") if p.is_file() and p.name != "complete")
    for path in files + [folder / "complete"]:
        s3.upload_file(str(path), bucket, prefix + path.relative_to(folder).as_posix())


def read_kv(text: str) -> dict[str, str]:
    """Parses `name=value` lines; lines without "=" are ignored."""
    return dict(line.split("=", 1) for line in text.splitlines() if "=" in line)


def write_fingerprint(fingerprint: dict[str, str]) -> str:
    """Formats a fingerprint as `name=value` lines in FINGERPRINT_FIELDS order."""
    return "".join(f"{name}={fingerprint[name]}\n" for name in FINGERPRINT_FIELDS)


def read_tsv(text: str) -> set[ClassRef]:
    """Parses `module<TAB>class` lines into class references."""
    return {tuple(line.split("\t", 1)) for line in text.splitlines() if "\t" in line}


def write_tsv(classes: set[ClassRef]) -> str:
    """Formats class references as sorted `module<TAB>class` lines."""
    return "".join(f"{module}\t{cls}\n" for module, cls in sorted(classes))


def find_modules(root: Path) -> list[str]:
    """Finds every Maven module: every source folder that has a pom.xml.

    Arguments:
        root: The source root.

    Returns:
        The module dirs relative to the root, with "/" separators and "." for the
        root itself, sorted.
    """
    modules = []
    for current, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in NOT_SOURCE_DIRS]
        if "pom.xml" in files:
            modules.append(Path(current).relative_to(root).as_posix() or ".")
    return sorted(modules)


def module_key(module: str) -> str:
    """Turns a module dir into one S3 key segment, for example services__workers."""
    return module.replace("/", "__")


def exclude_entry(cls: str) -> str:
    """Returns the Surefire excludes-file line that leaves out exactly one class.

    Surefire matches a path pattern such as `p/FooTest.java` at any depth, so it would
    also leave out `x.p.FooTest`; a `%regex[...]` entry must match the whole path.

    Arguments:
        cls: The fully qualified top-level class name.

    Returns:
        A line such as `%regex[org/sagebionetworks/FooTest\\.class]`.
    """
    return "%regex[" + cls.replace(".", "/") + r"\.class]"


def safe_file_name(build_id: str) -> str:
    """Turns a CodeBuild build id (`project:uuid`) into a portable file name."""
    return re.sub(r"[^A-Za-z0-9._-]", "_", build_id)


def report_outcome(path: Path) -> str:
    """Classifies one Surefire or Failsafe XML report.

    Arguments:
        path: The `TEST-<class>.xml` report.

    Returns:
        "failed" if the report shows a failure or an error or does not parse;
        "passed" if at least one test ran; otherwise "disabled".
    """
    try:
        suite = ET.parse(path).getroot()
        tests, skipped, failures, errors = (
            int(suite.get(attr, "0"))
            for attr in ("tests", "skipped", "failures", "errors")
        )
    except (ET.ParseError, ValueError):
        return "failed"
    if failures or errors:
        return "failed"
    return "passed" if tests - skipped > 0 else "disabled"


def classify_reports(root: Path) -> tuple[set[ClassRef], set[ClassRef]]:
    """Reads every Surefire and Failsafe report under the source root.

    Reports are grouped by top-level class: the class name up to the first "$". A
    class passed when at least one of its reports passed and none failed. A class
    whose tests were all disabled is in neither set, so it runs again next time.

    Arguments:
        root: The source root.

    Returns:
        The passed classes and the failed classes.
    """
    outcomes: dict[ClassRef, list[str]] = {}
    for current, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in NOT_SOURCE_DIRS - {"target"}]
        here = Path(current)
        if here.name not in REPORT_DIRS or here.parent.name != "target":
            continue
        module = here.parent.parent.relative_to(root).as_posix() or "."
        for name in files:
            if name.startswith("TEST-") and name.endswith(".xml"):
                top = name[len("TEST-") : -len(".xml")].split("$", 1)[0]
                outcome = report_outcome(here / name)
                outcomes.setdefault((module, top), []).append(outcome)
    passed = {
        cls
        for cls, results in outcomes.items()
        if "passed" in results and "failed" not in results
    }
    failed = {cls for cls, results in outcomes.items() if "failed" in results}
    return passed, failed


def load_builds(records: Path) -> list[BuildRecord]:
    """Reads the complete build records from a local copy of a run's S3 folder.

    A build with any record file missing (an unfinished upload, or a file that could
    not be downloaded) is left out.

    Arguments:
        records: The local copy of `reruns/<run id>/`, one subfolder per build.

    Returns:
        The complete builds, oldest first.
    """
    builds = []
    for folder in sorted(records.iterdir()) if records.is_dir() else []:
        files = {name: folder / name for name in RECORD_FILES}
        if not all(path.is_file() for path in files.values()):
            continue
        builds.append(
            BuildRecord(
                build_id=folder.name,
                folder=folder,
                completed=files["complete"].read_text().strip(),
                fingerprint=read_kv(files["fingerprint.txt"].read_text()),
                passed=read_tsv(files["passed.tsv"].read_text()),
                failed=read_tsv(files["failed.tsv"].read_text()),
                no_coverage=set(files["no-coverage.txt"].read_text().split()),
            )
        )
    return sorted(builds, key=lambda build: build.completed)


def remove_coverage_dirs(root: Path, modules: list[str]) -> None:
    """Deletes each module's coverage-data folder, if it exists."""
    for module in modules:
        shutil.rmtree(root / module / COVERAGE_DIR, ignore_errors=True)


def prepare(
    root: Path,
    env: dict[str, str],
    download: Downloader,
) -> None:
    """Runs the `prepare` command; never raises.

    Without an identity file (local and Jenkins builds), the feature is off. On any
    error the build runs the full suite with today's coverage path, and `record`
    records nothing because no state file is written.

    Arguments:
        root: The source root.
        env: The build's environment variables.
        download: Copies a run's S3 records to a local folder.
    """
    if not (root / IDENTITY_FILE).exists():
        (root / ARGS_FILE).write_text("")
        print("re-run records: off")
        return
    try:
        prepare_records(root, env, download)
    except Exception as e:
        print(f"re-run records: running the full suite and recording nothing ({e!r})")
        with contextlib.suppress(Exception):
            remove_coverage_dirs(root, find_modules(root))
        (root / STATE_FILE).unlink(missing_ok=True)
        (root / ARGS_FILE).write_text("")


def prepare_records(
    root: Path,
    env: dict[str, str],
    download: Downloader,
) -> None:
    """Chooses the classes to leave out and restores the earlier coverage data.

    A class is left out only if it passed in an earlier complete build of this run
    with the same fingerprint, and that build's passes count for the class's module:
    the build stored non-empty coverage data for the module, or the module's tests
    ran without a JaCoCo agent. Otherwise a killed test JVM, whose coverage file is
    empty, could leave classes out with no coverage data to check them against.

    Arguments:
        root: The source root.
        env: The build's environment variables.
        download: Copies a run's S3 records to a local folder.

    Raises:
        Exception: Any problem; `prepare` turns it into a full-suite build.
    """
    identity = read_kv((root / IDENTITY_FILE).read_text())
    run_id, bucket = identity["run_id"], identity["bucket"]
    build_id = env["CODEBUILD_BUILD_ID"]
    fingerprint = {"stack_builder_key": identity["stack_builder_key"]}
    fingerprint.update({f: env.get(var, "") for var, f in FINGERPRINT_ENV.items()})
    modules = find_modules(root)
    excluded: dict[str, set[str]] = {module: set() for module in modules}
    not_used: list[str] = []

    with tempfile.TemporaryDirectory(prefix="rerun-records-") as records:
        download(bucket, f"reruns/{run_id}/", Path(records))
        earlier = [b for b in load_builds(Path(records)) if b.build_id != build_id]
        kept = [b for b in earlier if b.fingerprint == fingerprint]
        restored: dict[str, list[BuildRecord]] = {module: [] for module in modules}
        for build in kept:
            for module in modules:
                proven = {cls for m, cls in build.passed if m == module}
                coverage = build.coverage_file(module)
                if coverage.is_file() and coverage.stat().st_size > 0:
                    restored[module].append(build)
                elif module not in build.no_coverage:
                    if proven:
                        not_used.append(f"{module} from {build.build_id} (no coverage)")
                    continue
                excluded[module] |= proven

        remove_coverage_dirs(root, modules)
        for module in modules:
            data_dir = root / module / COVERAGE_DIR
            data_dir.mkdir(parents=True)
            entries = [exclude_entry(cls) for cls in sorted(excluded[module])]
            lines = [INNER_CLASS_EXCLUDE] + entries
            (data_dir / "excludes.txt").write_text("\n".join(lines) + "\n")
            for build in restored[module]:
                restored_file = data_dir / f"{safe_file_name(build.build_id)}.exec"
                shutil.copyfile(build.coverage_file(module), restored_file)

    earlier_failed = set().union(*(build.failed for build in earlier))
    state = {
        "bucket": bucket,
        "run_id": run_id,
        "build_id": build_id,
        "fingerprint": fingerprint,
        "earlier_failed": sorted(earlier_failed),
        "excluded": {m: sorted(c) for m, c in excluded.items() if c},
    }
    (root / STATE_FILE).write_text(json.dumps(state))
    (root / ARGS_FILE).write_text(MAVEN_FLAG)
    print_banner(run_id, build_id, fingerprint, earlier, kept, excluded, not_used)


def print_banner(
    run_id: str,
    build_id: str,
    fingerprint: dict[str, str],
    earlier: list[BuildRecord],
    kept: list[BuildRecord],
    excluded: dict[str, set[str]],
    not_used: list[str],
) -> None:
    """Prints what `prepare` chose, before Maven starts.

    The log says "not re-run" rather than "skipped" because Surefire prints its own
    "Skipped" counts for disabled tests.

    Arguments:
        run_id: The GitHub workflow run id.
        build_id: This CodeBuild build's id.
        fingerprint: This build's inputs.
        earlier: The earlier complete builds of the run, oldest first.
        kept: The earlier builds with the same fingerprint.
        excluded: The classes left out, by module.
        not_used: Descriptions of earlier passes that were not used, and why.
    """
    print(f"re-run records: run {run_id}, build {build_id}")
    if kept:
        print("  using earlier builds: " + ", ".join(b.build_id for b in kept))
    elif earlier:
        latest = earlier[-1]
        changed = [
            field
            for field in FINGERPRINT_FIELDS
            if latest.fingerprint.get(field) != fingerprint[field]
        ]
        print(
            f"  full suite: build inputs changed since build {latest.build_id}: "
            + ", ".join(changed)
        )
    else:
        print("  full suite: no earlier build of this run")
    for module in sorted(m for m in excluded if excluded[m]):
        count = len(excluded[module])
        print(f"  {module}: {count} classes not re-run (passed in an earlier build)")
    for reason in not_used:
        print(f"  earlier passes not used: {reason}")
    print('  see "CI re-runs" in CONTRIBUTING.md')


def record(root: Path, upload: Uploader) -> None:
    """Runs the `record` command; never raises.

    Without the state file that a successful `prepare` writes, nothing is recorded.
    On any error the record has no `complete` file, so later builds ignore it.

    Arguments:
        root: The source root.
        upload: Copies a local folder to an S3 prefix, `complete` last.
    """
    state_file = root / STATE_FILE
    if not state_file.exists():
        print("re-run records: not recording")
        return
    try:
        record_build(root, json.loads(state_file.read_text()), upload)
    except Exception as e:
        print(f"re-run records: WARNING, this build's record was not saved ({e!r})")


def record_build(root: Path, state: dict, upload: Uploader) -> None:
    """Saves this build's record and prints what it found.

    Arguments:
        root: The source root.
        state: The state that `prepare` wrote.
        upload: Copies a local folder to an S3 prefix, `complete` last.

    Raises:
        Exception: Any problem; `record` logs it and saves nothing usable.
    """
    prefix = f"reruns/{state['run_id']}/{state['build_id']}/"
    passed, failed = classify_reports(root)
    earlier_failed = {tuple(cls) for cls in state["earlier_failed"]}
    rescued = sorted(passed & earlier_failed)
    first_time = sorted(failed - earlier_failed)
    excluded = state["excluded"]
    ran = passed | failed
    # The JaCoCo agent creates its file when a test JVM starts, so a module whose tests
    # ran without one has no agent (jacoco.skip) and its passes need no coverage data.
    no_coverage = sorted(
        {m for m, _ in ran if not (root / m / COVERAGE_DIR / CURRENT_EXEC).is_file()}
    )

    counts = []
    for module in sorted({m for m, _ in ran} | set(excluded)):
        counts.append(
            (
                module,
                len(excluded.get(module, [])),
                len({c for m, c in ran if m == module}),
                len({c for m, c in passed if m == module}),
                len({c for m, c in failed if m == module}),
            )
        )
    summary = ["module\tnot re-run\tran\tpassed\tfailed"]
    summary += ["\t".join(str(value) for value in row) for row in counts]
    summary += ["", "not re-run:"]
    summary += [f"{m}\t{c}" for m in sorted(excluded) for c in excluded[m]]
    summary += ["", "ran:"] + [f"{m}\t{c}" for m, c in sorted(ran)]
    summary += ["", "rescued:"] + [f"{m}\t{c}" for m, c in rescued]
    summary += ["", "first-time failures:"] + [f"{m}\t{c}" for m, c in first_time]

    with tempfile.TemporaryDirectory(prefix="rerun-records-") as scratch:
        folder = Path(scratch)
        completed = datetime.datetime.now(datetime.timezone.utc).isoformat()
        files = {
            "fingerprint.txt": write_fingerprint(state["fingerprint"]),
            "passed.tsv": write_tsv(passed),
            "failed.tsv": write_tsv(failed),
            "no-coverage.txt": "\n".join(no_coverage) + "\n",
            "summary.txt": "\n".join(summary) + "\n",
            "complete": completed + "\n",
        }
        for name, text in files.items():
            (folder / name).write_text(text)
        (folder / "coverage").mkdir()
        for module in find_modules(root):
            current = root / module / COVERAGE_DIR / CURRENT_EXEC
            # An empty file means the test JVM was killed before JaCoCo wrote its data.
            if current.is_file() and current.stat().st_size > 0:
                target = folder / "coverage" / f"{module_key(module)}.exec"
                shutil.copyfile(current, target)
        upload(state["bucket"], prefix, folder)

    print(f"re-run records: saved s3://{state['bucket']}/{prefix}")
    for module, not_rerun, ran_count, passed_count, failed_count in counts:
        print(
            f"  {module}: ran {ran_count}, passed {passed_count}, "
            f"failed {failed_count}, not re-run {not_rerun}"
        )
    rescued_names = ", ".join(cls for _, cls in rescued) or "none"
    first_time_names = ", ".join(cls for _, cls in first_time) or "none"
    print(f"  rescued (failed in an earlier build, passed now): {rescued_names}")
    print(f"  first-time failures: {first_time_names}")


def selftest() -> None:
    """Checks the rules against local fixtures, and the constants against the repo.

    Raises:
        AssertionError: If a rule or a constant is wrong.
    """
    repo = Path(__file__).resolve().parent.parent.parent
    template = (repo / "configuration/build/codepipeline_cf_template.yaml").read_text()
    action = template[template.index("- Name: BuildSynapseRepo") :]
    action = action[: action.index("BuildspecOverride")]
    variables = set(re.findall(r'"name":"(\w+)"', action))
    assert variables == set(FINGERPRINT_ENV) | NOT_FINGERPRINTED_ENV, (
        f"BuildSynapseRepo passes {sorted(variables)}: add each new variable to "
        "FINGERPRINT_ENV, or to NOT_FINGERPRINTED_ENV if it cannot change which "
        "tests pass"
    )

    with tempfile.TemporaryDirectory() as tmp:
        selftest_reports(Path(tmp) / "reports")
        selftest_prepare_and_record(Path(tmp) / "run")
    print("rerun_records selftest: passed")


def write_report(
    root: Path,
    module: str,
    cls: str,
    tests: int = 1,
    skipped: int = 0,
    failures: int = 0,
    errors: int = 0,
    folder: str = "surefire-reports",
    raw: str | None = None,
) -> None:
    """Writes a fixture `TEST-<class>.xml` report.

    Arguments:
        root: The fixture source root.
        module: The module dir.
        cls: The class name, which names the report file.
        tests: The `tests` attribute.
        skipped: The `skipped` attribute.
        failures: The `failures` attribute.
        errors: The `errors` attribute.
        folder: surefire-reports or failsafe-reports.
        raw: The report's whole content, instead of the attributes.
    """
    path = root / module / "target" / folder / f"TEST-{cls}.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    attributes = (
        f'tests="{tests}" skipped="{skipped}" failures="{failures}" errors="{errors}"'
    )
    path.write_text(raw if raw is not None else f"<testsuite {attributes}/>")


def selftest_reports(root: Path) -> None:
    """Checks what counts as passed and failed."""
    root.mkdir()
    write_report(root, "lib/a", "p.Pass")
    write_report(root, "lib/a", "p.Fail", failures=1)
    write_report(root, "lib/a", "p.Err", errors=1)
    write_report(root, "lib/a", "p.AllDisabled", tests=2, skipped=2)
    write_report(root, "lib/a", "p.SomeDisabled", tests=3, skipped=1)
    write_report(root, "lib/a", "p.Bad", raw="<testsuite")
    write_report(root, "lib/a", "p.Outer")
    write_report(root, "lib/a", "p.Outer$Inner", failures=1)
    write_report(root, "integration-test", "p.ITFoo", folder="failsafe-reports")
    passed, failed = classify_reports(root)
    assert passed == {
        ("lib/a", "p.Pass"),
        ("lib/a", "p.SomeDisabled"),
        ("integration-test", "p.ITFoo"),
    }, passed
    assert failed == {
        ("lib/a", cls) for cls in ("p.Fail", "p.Err", "p.Bad", "p.Outer")
    }, failed


def put_build(
    s3: Path,
    build_id: str,
    fingerprint: dict[str, str],
    completed: str,
    passed: list[ClassRef] = (),
    failed: list[ClassRef] = (),
    coverage: dict[str, bytes] | None = None,
    no_coverage: list[str] = (),
    missing: list[str] = (),
) -> None:
    """Writes a fixture build record into the fake S3 folder of run "R".

    Arguments:
        s3: The folder that stands in for the bucket.
        build_id: The build id.
        fingerprint: The build's inputs.
        completed: The content of the `complete` file.
        passed: The classes that passed.
        failed: The classes that failed.
        coverage: The coverage data, by module.
        no_coverage: The modules that ran without a JaCoCo agent.
        missing: Record files to leave out.
    """
    folder = s3 / "reruns" / "R" / build_id
    (folder / "coverage").mkdir(parents=True)
    files = {
        "fingerprint.txt": write_fingerprint(fingerprint),
        "passed.tsv": write_tsv(set(passed)),
        "failed.tsv": write_tsv(set(failed)),
        "no-coverage.txt": "\n".join(no_coverage),
        "complete": completed,
    }
    for name, text in files.items():
        if name not in missing:
            (folder / name).write_text(text)
    for module, data in (coverage or {}).items():
        (folder / "coverage" / f"{module_key(module)}.exec").write_bytes(data)


def run_quietly(command: Callable[..., None], *args: object) -> str:
    """Runs a command and returns what it printed."""
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        command(*args)
    return out.getvalue()


def selftest_prepare_and_record(tmp: Path) -> None:
    """Checks `prepare` and `record` against a fake bucket in a local folder."""
    root, s3 = tmp / "src", tmp / "s3"
    root.mkdir(parents=True)
    buckets = []

    def download(bucket: str, prefix: str, dest: Path) -> None:
        buckets.append(bucket)
        if (s3 / prefix).is_dir():
            shutil.copytree(s3 / prefix, dest, dirs_exist_ok=True)

    def upload(bucket: str, prefix: str, folder: Path) -> None:
        buckets.append(bucket)
        shutil.copytree(folder, s3 / prefix, dirs_exist_ok=True)

    def unreachable(bucket: str, prefix: str, dest: Path) -> None:
        raise OSError("S3 unreachable")

    def excludes(module: str) -> list[str]:
        return (root / module / COVERAGE_DIR / "excludes.txt").read_text().splitlines()

    def restored(module: str) -> list[str]:
        return sorted(p.name for p in (root / module / COVERAGE_DIR).glob("*.exec"))

    env = {"CODEBUILD_BUILD_ID": "proj:b9", "FULL_BUILD": "true", "EXTRA_ARGS": "-X"}
    same = {
        "stack_builder_key": "sb/abc.zip",
        "full_build": "true",
        "extra_args": "-X",
    }
    other = dict(same, extra_args="-Dfoo")

    # Feature off without an identity file.
    (root / "pom.xml").write_text("<project/>")
    run_quietly(prepare, root, env, download)
    assert (root / ARGS_FILE).read_text() == ""
    assert not (root / STATE_FILE).exists()

    # S3 unreachable: full suite, today's coverage path, nothing recorded.
    for module in ("lib/a", "services/workers", "integration-test", "nosrc"):
        (root / module).mkdir(parents=True)
        (root / module / "pom.xml").write_text("<project/>")
    identity = "run_id=R\nstack_builder_key=sb/abc.zip\nbucket=B\n"
    (root / IDENTITY_FILE).write_text(identity)
    (root / "lib/a" / COVERAGE_DIR).mkdir()
    out = run_quietly(prepare, root, env, unreachable)
    assert (root / ARGS_FILE).read_text() == "", out
    assert not (root / STATE_FILE).exists(), out
    assert not (root / "lib/a" / COVERAGE_DIR).exists()
    out = run_quietly(record, root, upload)
    assert out.startswith("re-run records: not recording"), out

    # No earlier build with the same inputs: full suite, records saved, and the
    # changed input named.
    put_build(
        s3,
        "proj:b0",
        other,
        "t0",
        passed=[("lib/a", "p.Old")],
        failed=[("lib/a", "p.G")],
        coverage={"lib/a": b"x"},
    )
    out = run_quietly(prepare, root, env, download)
    assert "extra_args" in out, out
    assert (root / ARGS_FILE).read_text() == MAVEN_FLAG
    assert excludes("lib/a") == [INNER_CLASS_EXCLUDE]
    assert buckets == ["B"], buckets

    # Two kept builds, plus builds that are ignored: other inputs (b0), no
    # 'complete' (b3), and a record file missing, as after a failed download (b4).
    put_build(
        s3,
        "proj:b1",
        same,
        "t1",
        passed=[
            ("lib/a", "p.A1"),
            ("services/workers", "p.W1"),
            ("integration-test", "p.I1"),
        ],
        failed=[("lib/a", "p.F")],
        coverage={"lib/a": b"cov-b1-a", "services/workers": b""},
        no_coverage=["integration-test"],
    )
    put_build(
        s3,
        "proj:b2",
        same,
        "t2",
        passed=[("lib/a", "p.A2"), ("lib/a", "p.Shared"), ("services/workers", "p.W2")],
        coverage={"lib/a": b"cov-b2-a", "services/workers": b"cov-b2-w"},
    )
    put_build(
        s3,
        "proj:b3",
        same,
        "t3",
        passed=[("lib/a", "p.NoComplete")],
        missing=["complete"],
    )
    put_build(
        s3,
        "proj:b4",
        same,
        "t4",
        passed=[("lib/a", "p.NoFingerprint")],
        missing=["fingerprint.txt"],
    )
    (root / "nosrc" / COVERAGE_DIR / "stale.exec").write_bytes(b"stale")
    out = run_quietly(prepare, root, env, download)

    lib_a = [exclude_entry(cls) for cls in ("p.A1", "p.A2", "p.Shared")]
    assert excludes("lib/a") == [INNER_CLASS_EXCLUDE] + lib_a, out
    assert exclude_entry("p.A1") == r"%regex[p/A1\.class]"
    # b1 stored empty services/workers coverage, so its passes there are not used.
    assert excludes("services/workers") == [INNER_CLASS_EXCLUDE, exclude_entry("p.W2")]
    assert "services/workers from proj:b1 (no coverage)" in out, out
    # integration-test ran without a JaCoCo agent in b1, so its passes count.
    assert excludes("integration-test") == [INNER_CLASS_EXCLUDE, exclude_entry("p.I1")]
    assert excludes("nosrc") == excludes(".") == [INNER_CLASS_EXCLUDE]
    assert restored("lib/a") == ["proj_b1.exec", "proj_b2.exec"]
    assert restored("services/workers") == ["proj_b2.exec"]
    assert restored("nosrc") == []
    # JaCoCo skips its check when a module has no coverage data at all, so every
    # module with classes left out has restored data or no JaCoCo agent.
    for module in find_modules(root):
        if len(excludes(module)) > 1:
            assert restored(module) or module == "integration-test", module

    # Record: rescued classes come from any earlier build; empty coverage is not
    # uploaded; a module that ran without a coverage file has no JaCoCo agent.
    write_report(root, "lib/a", "p.F")
    write_report(root, "lib/a", "p.G")
    write_report(root, "lib/a", "p.N", failures=1)
    write_report(root, "services/workers", "p.W3")
    write_report(root, "integration-test", "p.I2", folder="failsafe-reports")
    (root / "lib/a" / COVERAGE_DIR / CURRENT_EXEC).write_bytes(b"cov-b9-a")
    (root / "services/workers" / COVERAGE_DIR / CURRENT_EXEC).write_bytes(b"")
    out = run_quietly(record, root, upload)
    saved = s3 / "reruns" / "R" / "proj:b9"
    assert (saved / "complete").read_text().strip(), out
    assert (saved / "coverage" / "lib__a.exec").read_bytes() == b"cov-b9-a"
    assert not (saved / "coverage" / "services__workers.exec").exists()
    assert (saved / "no-coverage.txt").read_text().split() == ["integration-test"]
    assert read_tsv((saved / "passed.tsv").read_text()) == {
        ("lib/a", "p.F"),
        ("lib/a", "p.G"),
        ("services/workers", "p.W3"),
        ("integration-test", "p.I2"),
    }
    assert read_kv((saved / "fingerprint.txt").read_text()) == same
    assert "rescued (failed in an earlier build, passed now): p.F, p.G" in out, out
    assert "first-time failures: p.N" in out, out
    assert buckets[-1] == "B", buckets


def main(argv: list[str]) -> int:
    """Runs the command named on the command line.

    Arguments:
        argv: The command line: the script, then prepare, record or selftest.

    Returns:
        0, or 2 for an unknown command.
    """
    command = argv[1] if len(argv) > 1 else ""
    if command == "prepare":
        prepare(Path.cwd(), dict(os.environ), download_records)
    elif command == "record":
        record(Path.cwd(), upload_records)
    elif command == "selftest":
        selftest()
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
