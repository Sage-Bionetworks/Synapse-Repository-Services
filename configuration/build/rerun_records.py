#!/usr/bin/env python3
"""Lets a re-run of a GitHub workflow run leave out test classes that already passed in an
earlier CodeBuild build of the same run (see "CI re-runs" in CONTRIBUTING.md).

  prepare   Before Maven: choose the classes to leave out and restore earlier coverage data.
  record    After Maven: save this build's passed classes and coverage data to S3.
  selftest  Check the rules against local fixtures, without AWS or Docker.

Any problem makes the build run more tests, never fewer, and neither command changes the
build result: both always exit 0.
"""
import contextlib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

IMAGE = "maven:3-amazoncorretto-21"
IDENTITY_FILE = "rerun-identity.txt"
ARGS_FILE = "rerun-maven-args.txt"
STATE_FILE = "rerun-state.json"
COVERAGE_DIR = "coverage-data"
CURRENT_EXEC = "current.exec"
MAVEN_FLAG = "-Dsynapse.rerun"
# Surefire drops its default inner-class exclude once an excludes file is given.
INNER_CLASS_EXCLUDE = "**/*$*"
JACOCO_SKIP = "<jacoco.skip>true</jacoco.skip>"
REPORT_DIRS = ("surefire-reports", "failsafe-reports")
NOT_SOURCE_DIRS = {".git", ".m2", "target", "node_modules", COVERAGE_DIR}
FINGERPRINT_FIELDS = ("stack_builder_key", "full_build", "extra_args", "image_digest")


class S3Store:
    """The run's records in S3, through the AWS CLI on the CodeBuild host."""

    def __init__(self, bucket):
        self.bucket = bucket

    def list(self, prefix):
        """Returns {key: last modified} for every object under the prefix."""
        out = subprocess.run(
            ["aws", "s3api", "list-objects-v2", "--bucket", self.bucket, "--prefix", prefix,
             "--query", "Contents[].[Key,LastModified]", "--output", "json"],
            check=True, capture_output=True, text=True).stdout.strip()
        return {key: modified for key, modified in (json.loads(out) if out and out != "null" else [])}

    def get(self, key, dest):
        """Downloads one object; returns False if it could not be downloaded."""
        result = subprocess.run(
            ["aws", "s3", "cp", "--only-show-errors", f"s3://{self.bucket}/{key}", str(dest)])
        return result.returncode == 0

    def put(self, src, key):
        subprocess.run(
            ["aws", "s3", "cp", "--only-show-errors", str(src), f"s3://{self.bucket}/{key}"],
            check=True)


class LocalStore:
    """A folder standing in for S3 in the self-check."""

    def __init__(self, folder):
        self.folder = Path(folder)

    def list(self, prefix):
        found = {}
        for path in self.folder.rglob("*"):
            key = path.relative_to(self.folder).as_posix()
            if path.is_file() and key.startswith(prefix):
                found[key] = "%020.6f" % path.stat().st_mtime
        return found

    def get(self, key, dest):
        src = self.folder / key
        if not src.is_file():
            return False
        shutil.copyfile(src, dest)
        return True

    def put(self, src, key):
        dest = self.folder / key
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)


def read_image_digest():
    subprocess.run(["docker", "pull", "-q", IMAGE], check=True, stdout=subprocess.DEVNULL)
    return subprocess.run(
        ["docker", "image", "inspect", "--format", "{{index .RepoDigests 0}}", IMAGE],
        check=True, capture_output=True, text=True).stdout.strip()


def read_kv(text):
    return dict(line.split("=", 1) for line in text.splitlines() if "=" in line)


def write_kv(values):
    return "".join(f"{name}={values[name]}\n" for name in FINGERPRINT_FIELDS)


def read_tsv(text):
    return {tuple(line.split("\t", 1)) for line in text.splitlines() if "\t" in line}


def write_tsv(rows):
    return "".join(f"{module}\t{cls}\n" for module, cls in sorted(rows))


def walk_source(root):
    """Yields (dir, file names) for every source folder, skipping build output and caches."""
    for current, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in NOT_SOURCE_DIRS]
        yield Path(current), files


def find_modules(root):
    """Returns {module dir: pom text}; a module dir is relative to the root, '.' for the root."""
    return {(Path(current).relative_to(root).as_posix() or "."): (Path(current) / "pom.xml").read_text()
            for current, files in walk_source(root) if "pom.xml" in files}


def module_key(module):
    return module.replace("/", "__")


def exclude_entry(cls):
    # An anchored regex: a path pattern such as p/FooTest.java would also match x/p/FooTest.
    return "%regex[" + cls.replace(".", "/") + r"\.class]"


def safe_file_name(build_id):
    return re.sub(r"[^A-Za-z0-9._-]", "_", build_id)


def classify_reports(root):
    """Returns (passed, failed) sets of (module, top-level class) from Surefire/Failsafe reports.

    A report passed when it parses, ran at least one test, and has no failures or errors. A
    class passed when one of its reports (its own or a nested class's) passed and none failed.
    A class whose tests were all disabled is in neither set, so it runs again next time.
    """
    outcomes = {}
    for current, dirs, files in os.walk(root):
        dirs[:] = [d for d in dirs if d not in NOT_SOURCE_DIRS - {"target"}]
        here = Path(current)
        if here.name not in REPORT_DIRS or here.parent.name != "target":
            continue
        module = here.parent.parent.relative_to(root).as_posix() or "."
        for name in files:
            if not (name.startswith("TEST-") and name.endswith(".xml")):
                continue
            top = name[len("TEST-"):-len(".xml")].split("$", 1)[0]
            outcomes.setdefault((module, top), []).append(report_outcome(here / name))
    passed = {c for c, results in outcomes.items() if "passed" in results and "failed" not in results}
    failed = {c for c, results in outcomes.items() if "failed" in results}
    return passed, failed


def report_outcome(path):
    try:
        suite = ET.parse(path).getroot()
        tests, skipped, failures, errors = (
            int(suite.get(attr, "0")) for attr in ("tests", "skipped", "failures", "errors"))
    except (ET.ParseError, ValueError):
        return "failed"
    if failures or errors:
        return "failed"
    return "passed" if tests - skipped > 0 else "disabled"


def remove_coverage_dirs(root, modules):
    for module in modules:
        shutil.rmtree(root / module / COVERAGE_DIR, ignore_errors=True)


def prepare(root, env, open_store, image_digest):
    if not (root / IDENTITY_FILE).exists():
        (root / ARGS_FILE).write_text("")
        print("re-run records: off")
        return
    try:
        prepare_records(root, env, open_store, image_digest)
    except Exception as e:  # any problem: full suite, today's coverage path, nothing recorded
        print(f"re-run records: running the full suite and recording nothing ({e!r})")
        with contextlib.suppress(Exception):
            remove_coverage_dirs(root, find_modules(root))
        (root / STATE_FILE).unlink(missing_ok=True)
        (root / ARGS_FILE).write_text("")


def prepare_records(root, env, open_store, image_digest):
    identity = read_kv((root / IDENTITY_FILE).read_text())
    run_id, build_id = identity["run_id"], env["CODEBUILD_BUILD_ID"]
    bucket = "synapse-build-source-" + env["user"]
    digest = image_digest()
    if not digest:
        raise ValueError(f"no image digest for {IMAGE}")
    fingerprint = {"stack_builder_key": identity["stack_builder_key"],
                   "full_build": env.get("FULL_BUILD", ""),
                   "extra_args": env.get("EXTRA_ARGS", ""),
                   "image_digest": digest}
    store = open_store(bucket)
    run_prefix = f"reruns/{run_id}/"
    listing = store.list(run_prefix)
    complete = sorted((modified, key[len(run_prefix):-len("/complete")])
                      for key, modified in listing.items()
                      if key.endswith("/complete") and key.count("/") == 3)
    earlier = [build for _, build in complete if build != build_id]
    modules = find_modules(root)
    downloads = Path(tempfile.mkdtemp(prefix="rerun-records-"))

    def fetch_text(build, name):
        dest = downloads / safe_file_name(build) / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not store.get(f"{run_prefix}{build}/{name}", dest):
            raise IOError(f"cannot download {name} of build {build}")
        return dest.read_text()

    earlier_failed, kept, fingerprints = set(), [], {}
    for build in earlier:
        fingerprints[build] = read_kv(fetch_text(build, "fingerprint.txt"))
        earlier_failed |= read_tsv(fetch_text(build, "failed.tsv"))
        if fingerprints[build] == fingerprint:
            kept.append(build)

    excluded = {module: set() for module in modules}
    restored = {module: [] for module in modules}
    unused = []
    for build in kept:
        passed = read_tsv(fetch_text(build, "passed.tsv"))
        for module, pom in modules.items():
            coverage = downloads / safe_file_name(build) / f"{module_key(module)}.exec"
            key = f"{run_prefix}{build}/coverage/{module_key(module)}.exec"
            has_coverage = key in listing and store.get(key, coverage) and coverage.stat().st_size > 0
            proven = {cls for m, cls in passed if m == module}
            if has_coverage:
                restored[module].append((build, coverage))
            elif JACOCO_SKIP not in pom:
                if proven:
                    unused.append(f"{module} from build {build} (no coverage data)")
                continue
            excluded[module] |= proven

    remove_coverage_dirs(root, modules)
    for module in modules:
        data_dir = root / module / COVERAGE_DIR
        data_dir.mkdir(parents=True)
        lines = [INNER_CLASS_EXCLUDE] + [exclude_entry(cls) for cls in sorted(excluded[module])]
        (data_dir / "excludes.txt").write_text("\n".join(lines) + "\n")
        for build, coverage in restored[module]:
            shutil.copyfile(coverage, data_dir / f"{safe_file_name(build)}.exec")
    shutil.rmtree(downloads, ignore_errors=True)

    (root / STATE_FILE).write_text(json.dumps({
        "bucket": bucket, "run_id": run_id, "build_id": build_id, "fingerprint": fingerprint,
        "kept": kept, "earlier_failed": sorted(earlier_failed),
        "excluded": {m: sorted(c) for m, c in excluded.items() if c}}))
    (root / ARGS_FILE).write_text(MAVEN_FLAG)

    print(f"re-run records: run {run_id}, build {build_id}")
    if kept:
        print("  using earlier builds: " + ", ".join(kept))
    elif earlier:
        latest = fingerprints[earlier[-1]]
        changed = [name for name in FINGERPRINT_FIELDS if latest.get(name) != fingerprint[name]]
        print(f"  full suite: build inputs changed since build {earlier[-1]}: " + ", ".join(changed))
    else:
        print("  full suite: no earlier build of this run")
    for module in sorted(m for m in excluded if excluded[m]):
        print(f"  {module}: {len(excluded[module])} classes not re-run (passed in an earlier build)")
    for reason in unused:
        print(f"  earlier passes not used: {reason}")
    print('  see "CI re-runs" in CONTRIBUTING.md')


def record(root, open_store):
    state_file = root / STATE_FILE
    if not state_file.exists():
        print("re-run records: not recording")
        return
    try:
        record_build(root, json.loads(state_file.read_text()), open_store)
    except Exception as e:  # never change the build result; without 'complete' this build is ignored
        print(f"re-run records: WARNING, this build's record was not saved ({e!r})")


def record_build(root, state, open_store):
    store = open_store(state["bucket"])
    prefix = f"reruns/{state['run_id']}/{state['build_id']}/"
    passed, failed = classify_reports(root)
    earlier_failed = {tuple(row) for row in state["earlier_failed"]}
    rescued = sorted(passed & earlier_failed)
    first_time = sorted(failed - earlier_failed)
    excluded = state["excluded"]

    summary = ["module\tnot re-run\tran\tpassed\tfailed"]
    reported = {m for m, _ in passed | failed} | set(excluded)
    for module in sorted(reported):
        counts = [len(excluded.get(module, [])), len({c for m, c in passed | failed if m == module}),
                  len({c for m, c in passed if m == module}), len({c for m, c in failed if m == module})]
        summary.append("\t".join([module] + [str(n) for n in counts]))
    summary += ["", "not re-run:"] + [f"{m}\t{c}" for m in sorted(excluded) for c in excluded[m]]
    summary += ["", "ran:"] + [f"{m}\t{c}" for m, c in sorted(passed | failed)]
    summary += ["", "rescued:"] + [f"{m}\t{c}" for m, c in rescued]
    summary += ["", "first-time failures:"] + [f"{m}\t{c}" for m, c in first_time]

    with tempfile.TemporaryDirectory(prefix="rerun-records-") as scratch:
        files = {"fingerprint.txt": write_kv(state["fingerprint"]), "passed.tsv": write_tsv(passed),
                 "failed.tsv": write_tsv(failed), "summary.txt": "\n".join(summary) + "\n"}
        for name, text in files.items():
            (Path(scratch) / name).write_text(text)
            store.put(Path(scratch) / name, prefix + name)
        for module in find_modules(root):
            current = root / module / COVERAGE_DIR / CURRENT_EXEC
            if current.is_file() and current.stat().st_size > 0:
                store.put(current, f"{prefix}coverage/{module_key(module)}.exec")
        (Path(scratch) / "complete").write_text("")
        store.put(Path(scratch) / "complete", prefix + "complete")

    print(f"re-run records: saved s3://{state['bucket']}/{prefix}")
    for line in summary[1:len(reported) + 1]:
        module, not_rerun, ran, ok, bad = line.split("\t")
        print(f"  {module}: ran {ran}, passed {ok}, failed {bad}, not re-run {not_rerun}")
    print("  rescued (failed in an earlier build, passed now): " + (", ".join(c for _, c in rescued) or "none"))
    print("  first-time failures: " + (", ".join(c for _, c in first_time) or "none"))


def selftest():
    script_dir = Path(__file__).resolve().parent
    docker_build = script_dir.parent.parent / "docker_build.sh"
    assert IMAGE in docker_build.read_text(), f"{docker_build} no longer uses {IMAGE}"

    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        selftest_reports(tmp / "reports")
        selftest_prepare_and_record(tmp / "run")
    print("rerun_records selftest: passed")


def make_source(root):
    poms = {".": "<project/>", "lib/a": "<project/>", "services/workers": "<project/>",
            "integration-test": f"<project><properties>{JACOCO_SKIP}</properties></project>",
            "nosrc": "<project/>"}
    for module, pom in poms.items():
        (root / module).mkdir(parents=True, exist_ok=True)
        (root / module / "pom.xml").write_text(pom)
    (root / IDENTITY_FILE).write_text("run_id=R\nstack_builder_key=sb/abc.zip\n")


def write_report(root, module, cls, tests=1, skipped=0, failures=0, errors=0, folder="surefire-reports", raw=None):
    path = root / module / "target" / folder / f"TEST-{cls}.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(raw if raw is not None else
                    f'<testsuite name="{cls}" tests="{tests}" skipped="{skipped}" '
                    f'failures="{failures}" errors="{errors}"/>')


def selftest_reports(root):
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
    assert passed == {("lib/a", "p.Pass"), ("lib/a", "p.SomeDisabled"), ("integration-test", "p.ITFoo")}, passed
    assert failed == {("lib/a", c) for c in ("p.Fail", "p.Err", "p.Bad", "p.Outer")}, failed


def put_build(store_dir, build, fingerprint, passed=(), failed=(), coverage=None, complete=True, age=0):
    folder = store_dir / "reruns" / "R" / build
    (folder / "coverage").mkdir(parents=True)
    (folder / "fingerprint.txt").write_text(write_kv(fingerprint))
    (folder / "passed.tsv").write_text(write_tsv(passed))
    (folder / "failed.tsv").write_text(write_tsv(failed))
    for module, data in (coverage or {}).items():
        (folder / "coverage" / f"{module_key(module)}.exec").write_bytes(data)
    if complete:
        (folder / "complete").write_text("")
        os.utime(folder / "complete", (1000 + age, 1000 + age))


def run_quietly(fn, *args):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        fn(*args)
    return out.getvalue()


def selftest_prepare_and_record(tmp):
    root, store_dir = tmp / "src", tmp / "s3"
    root.mkdir(parents=True)
    store_dir.mkdir()
    open_store = lambda bucket: LocalStore(store_dir)
    env = {"CODEBUILD_BUILD_ID": "proj:b9", "user": "u", "FULL_BUILD": "true", "EXTRA_ARGS": "-X"}
    digest = lambda: "maven@sha256:1"
    same = {"stack_builder_key": "sb/abc.zip", "full_build": "true", "extra_args": "-X", "image_digest": "maven@sha256:1"}
    other = dict(same, extra_args="-Dfoo")

    # Feature off without an identity file.
    (root / "pom.xml").write_text("<project/>")
    run_quietly(prepare, root, env, open_store, digest)
    assert (root / ARGS_FILE).read_text() == "" and not (root / STATE_FILE).exists()

    # Unreadable image digest: full suite, today's coverage path, nothing recorded.
    make_source(root)
    (root / "lib/a" / COVERAGE_DIR).mkdir()
    out = run_quietly(prepare, root, env, open_store, lambda: "")
    assert (root / ARGS_FILE).read_text() == "" and not (root / STATE_FILE).exists(), out
    assert not (root / "lib/a" / COVERAGE_DIR).exists()
    assert run_quietly(record, root, open_store).startswith("re-run records: not recording")

    # No earlier build with the same inputs: full suite, records saved, the changed input named.
    put_build(store_dir, "proj:b0", other, passed=[("lib/a", "p.Old")], failed=[("lib/a", "p.G")],
              coverage={"lib/a": b"x"}, age=0)
    out = run_quietly(prepare, root, env, open_store, digest)
    assert "extra_args" in out and (root / ARGS_FILE).read_text() == MAVEN_FLAG, out
    assert (root / "lib/a" / COVERAGE_DIR / "excludes.txt").read_text() == INNER_CLASS_EXCLUDE + "\n"

    # Two kept builds, plus one with other inputs and one without 'complete' (both ignored).
    put_build(store_dir, "proj:b1", same, age=1,
              passed=[("lib/a", "p.A1"), ("services/workers", "p.W1"), ("integration-test", "p.I1")],
              failed=[("lib/a", "p.F")], coverage={"lib/a": b"cov-b1-a", "services/workers": b""})
    put_build(store_dir, "proj:b2", same, age=2,
              passed=[("lib/a", "p.A2"), ("lib/a", "p.Shared"), ("services/workers", "p.W2")],
              coverage={"lib/a": b"cov-b2-a", "services/workers": b"cov-b2-w"})
    put_build(store_dir, "proj:b3", same, passed=[("lib/a", "p.Incomplete")], complete=False)
    (root / "nosrc" / COVERAGE_DIR / "stale.exec").write_bytes(b"stale")
    out = run_quietly(prepare, root, env, open_store, digest)

    def excludes(module):
        return (root / module / COVERAGE_DIR / "excludes.txt").read_text().splitlines()

    def restored(module):
        return sorted(p.name for p in (root / module / COVERAGE_DIR).glob("*.exec"))

    assert excludes("lib/a") == [INNER_CLASS_EXCLUDE] + [exclude_entry(c) for c in ("p.A1", "p.A2", "p.Shared")], out
    assert exclude_entry("p.A1") == r"%regex[p/A1\.class]"
    # Build b1 stored empty services/workers coverage, so its passes there are not used.
    assert excludes("services/workers") == [INNER_CLASS_EXCLUDE, exclude_entry("p.W2")]
    assert excludes("integration-test") == [INNER_CLASS_EXCLUDE, exclude_entry("p.I1")]
    assert excludes("nosrc") == excludes(".") == [INNER_CLASS_EXCLUDE]
    assert restored("lib/a") == ["proj_b1.exec", "proj_b2.exec"]
    assert restored("services/workers") == ["proj_b2.exec"] and restored("nosrc") == []
    assert "services/workers from build proj:b1 (no coverage data)" in out, out
    modules = find_modules(root)
    for module in modules:  # JaCoCo skips its check when a module has no coverage data at all
        if len(excludes(module)) > 1:
            assert restored(module) or JACOCO_SKIP in modules[module], module

    # Record: rescued classes come from any earlier build; empty coverage is not uploaded.
    write_report(root, "lib/a", "p.F")
    write_report(root, "lib/a", "p.G")
    write_report(root, "lib/a", "p.N", failures=1)
    (root / "lib/a" / COVERAGE_DIR / CURRENT_EXEC).write_bytes(b"cov-b9-a")
    (root / "services/workers" / COVERAGE_DIR / CURRENT_EXEC).write_bytes(b"")
    out = run_quietly(record, root, open_store)
    saved = store_dir / "reruns" / "R" / "proj:b9"
    assert (saved / "complete").exists(), out
    assert (saved / "coverage" / "lib__a.exec").read_bytes() == b"cov-b9-a"
    assert not (saved / "coverage" / "services__workers.exec").exists()
    assert read_tsv((saved / "passed.tsv").read_text()) == {("lib/a", "p.F"), ("lib/a", "p.G")}
    assert read_kv((saved / "fingerprint.txt").read_text()) == same
    assert "rescued (failed in an earlier build, passed now): p.F, p.G" in out, out
    assert "first-time failures: p.N" in out, out


def main(argv):
    command = argv[1] if len(argv) > 1 else ""
    if command == "prepare":
        prepare(Path.cwd(), os.environ, S3Store, read_image_digest)
    elif command == "record":
        record(Path.cwd(), S3Store)
    elif command == "selftest":
        selftest()
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
