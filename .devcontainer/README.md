# Synapse Repository Services devcontainer

A JDK 21 + Maven workspace container and a local MySQL 8.4 sidecar that replaces the manual JDK/Maven/MySQL/`settings.xml`/AWS-config steps of the Confluence "Developer Bootstrap" page.

## Prerequisites

- Docker (Docker Desktop, Rancher Desktop, or Engine with Compose v2) with at least 4 CPUs and 16 GB of memory allocated.
- One of: VS Code with the Dev Containers extension, IntelliJ IDEA with Dev Containers support, or GitHub Codespaces.
- Eclipse is not supported; use the Confluence Developer Bootstrap page instead.
- Windows: clone inside WSL2, or use "Clone Repository in Container Volume", for filesystem performance.

## One-time setup

- Be a member of the `synapse-developers` GitHub team.
- Build your personal dev stack with the `build_synapse_dev_stack.yml` [workflow](https://github.com/Sage-Bionetworks/synapse-ops-dev/actions/workflows/build_synapse_dev_stack.yml) in synapse-ops-dev. The instance name you choose (at most 7 characters) is your `DEVELOPER_NAME`.

## First start

```
cp .devcontainer/.env.example .devcontainer/.env
# edit .devcontainer/.env and set DEVELOPER_NAME
```

Then "Reopen in Container". The `postCreateCommand` (`setup.sh`) creates your databases, writes `~/.m2/settings.xml` and, if absent, `~/.aws/config`.

Codespaces: add a user secret `DEVELOPER_NAME` for this repository instead. If postCreate still reports it unset, create `.devcontainer/.env` in the codespace and re-run `bash .devcontainer/setup.sh`.

## AWS

```
aws sso login --use-device-code
```

The token cache lives in the shared `synapse-aws-config` volume, so one login covers every worktree/clone.

## Build and test

```
mvn clean install -DskipTests
mvn test -pl <module>
```

Most Spring-autowired tests need your AWS dev stack and a valid SSO session.

## Run the services

```
cd integration-test && mvn cargo:run
```

Then open `http://localhost:8080/services-repository-develop-SNAPSHOT/repo/v1/version`.

## Optional settings

Copy `settings.properties.example` to `settings.properties` (gitignored) to add or override `settings.xml` properties, for example Google Cloud credentials. Re-run `bash .devcontainer/setup.sh` afterwards.

## Reset databases

```
bash .devcontainer/setup.sh --reset-db
```

Many tests assume a clean database.

## MySQL

MySQL is pinned to the production version (`Constants.RDS_ENGINE_VERSION` in Synapse-Stack-Builder) with the production parameter-group flags. When bumping, update `image:` in `docker-compose.yml`. Downgrades require deleting the `mysql-data` volume.

## Codespaces

No prebuilds are configured. If the team adopts Codespaces, consider prebuilds on `develop` (needs org admin; costs Actions minutes and storage).

## Git and worktrees

Git is not usable inside the container for `git worktree` checkouts: their `.git` file points at a host path that is not mounted. Commit from the host. Normal clones work inside the container, except pre-commit hooks, which are not installed in the image; commit from the host if you use `pre-commit install`.

## Changing DEVELOPER_NAME

Edit `.env`, re-run `bash .devcontainer/setup.sh`, then `mvn clean install -DskipTests` (`stack.properties` is filtered at build time).

## Troubleshooting

- postCreate failed: read the message, fix `.env`, re-run `bash .devcontainer/setup.sh`.
- Build fails connecting to the database: on the host run `docker compose -f .devcontainer/docker-compose.yml ps` (the container has no Docker CLI) and check that `mysql8` is healthy.
