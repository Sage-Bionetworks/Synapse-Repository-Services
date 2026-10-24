#!/usr/bin/env bash
# Prepares the devcontainer: local MySQL databases, ~/.m2/settings.xml and ~/.aws/config.
# Runs inside the workspace container (postCreateCommand). Idempotent; safe to re-run.
#
# Usage: bash .devcontainer/setup.sh [--reset-db]
set -euo pipefail

usage() {
  echo "Usage: bash .devcontainer/setup.sh [--reset-db]" >&2
}

RESET_DB=false
case "${1:-}" in
  "") ;;
  --reset-db) RESET_DB=true ;;
  *) usage; exit 2 ;;
esac
if [ "$#" -gt 1 ]; then
  usage
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
SETTINGS_PROPERTIES="$SCRIPT_DIR/settings.properties"
M2_SETTINGS="$HOME/.m2/settings.xml"
AWS_CONFIG="$HOME/.aws/config"
SSO_ACCT_NUM=449435941126

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

xml_escape() {
  local s="$1"
  s="${s//&/&amp;}"
  s="${s//</&lt;}"
  s="${s//>/&gt;}"
  printf '%s' "$s"
}

# --- 1. Resolve DEVELOPER_NAME: .env file first, then the container environment (Codespaces secret).
DEV_NAME=""
if [ -f "$ENV_FILE" ]; then
  line="$(grep -E '^DEVELOPER_NAME=' "$ENV_FILE" | tail -n 1 || true)"
  if [ -n "$line" ]; then
    value="${line#*=}"
    value="${value//$'\r'/}"
    value="$(trim "$value")"
    value="${value#\"}"; value="${value%\"}"
    value="${value#\'}"; value="${value%\'}"
    DEV_NAME="$(trim "$value")"
  fi
fi
if [ -z "$DEV_NAME" ]; then
  DEV_NAME="$(trim "${DEVELOPER_NAME:-}")"
fi

# --- 2. Validate.
if [ -z "$DEV_NAME" ]; then
  echo "DEVELOPER_NAME is not set. Copy .devcontainer/.env.example to .devcontainer/.env and set it to your AWS dev stack instance name (or set a Codespaces secret DEVELOPER_NAME), then run: bash .devcontainer/setup.sh" >&2
  exit 1
fi
if ! [[ "$DEV_NAME" =~ ^[a-z][a-z0-9]{0,6}$ ]]; then
  echo "DEVELOPER_NAME '$DEV_NAME' is invalid: use 1-7 lowercase letters/digits starting with a letter (your dev stack instance name)." >&2
  exit 1
fi
DB_MAIN="dev${DEV_NAME}"
DB_TABLES="dev${DEV_NAME}tables"
DB_USER="dev${DEV_NAME}"
DB_PASSWORD="platform"

# --- 3. Repair ownership of named volumes created as root (or before a UID remap).
for dir in "$HOME/.m2/repository" "$HOME/.aws"; do
  if [ -d "$dir" ] && [ "$(stat -c %u "$dir")" != "$(id -u)" ]; then
    sudo chown -R "$(id -u):$(id -g)" "$dir"
  fi
done
mkdir -p "$HOME/.m2" "$HOME/.aws"

# --- 4. Wait for MySQL.
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:-root}"
mysql_root() {
  mysql -h mysql8 -uroot "$@"
}
ready=false
for _ in $(seq 1 120); do
  if mysql_root -e 'SELECT 1' >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
if [ "$ready" != true ]; then
  echo "MySQL at mysql8:3306 is not reachable" >&2
  exit 1
fi

# --- 5. Optional reset.
if [ "$RESET_DB" = true ]; then
  mysql_root -e "DROP DATABASE IF EXISTS \`${DB_MAIN}\`; DROP DATABASE IF EXISTS \`${DB_TABLES}\`;"
  echo "Dropped databases ${DB_MAIN} and ${DB_TABLES}"
fi

# --- 6. Databases and user. The user needs GRANT OPTION because DBUserHelper creates DB users.
mysql_root <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_MAIN}\`;
CREATE DATABASE IF NOT EXISTS \`${DB_TABLES}\`;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO '${DB_USER}'@'%' WITH GRANT OPTION;
SQL

# --- 7. ~/.m2/settings.xml. Read by SettingsLoader and, via the active profile, forwarded by cargo.
JDBC_URL="jdbc:mysql://mysql8:3306/${DB_MAIN}?allowPublicKeyRetrieval=true&useSSL=false"
declare -A PROPS=()
KEYS=()
set_prop() {
  if [ -z "${PROPS[$1]+x}" ]; then
    KEYS+=("$1")
  fi
  PROPS["$1"]="$2"
}

set_prop org.sagebionetworks.stack dev
set_prop org.sagebionetworks.developer "$DEV_NAME"
set_prop org.sagebionetworks.stack.instance "$DEV_NAME"
set_prop org.sagebionetworks.repository.database.connection.url "$JDBC_URL"
set_prop org.sagebionetworks.repository.database.username "$DB_USER"
set_prop org.sagebionetworks.repository.database.password "$DB_PASSWORD"
set_prop org.sagebionetworks.id.generator.database.connection.url "$JDBC_URL"
set_prop org.sagebionetworks.id.generator.database.username "$DB_USER"
set_prop org.sagebionetworks.id.generator.database.password "$DB_PASSWORD"
set_prop org.sagebionetworks.table.cluster.endpoint.0 mysql8
set_prop org.sagebionetworks.table.cluster.schema.0 "$DB_TABLES"
set_prop org.sagebionetworks.google.cloud.enabled false
set_prop org.sagebionetworks.search.enabled false
set_prop org.sagebionetworks.doi.datacite.enabled false
set_prop org.sagebionetworks.docusign.enabled false

# Every property the integration-test cargo plugin forwards must be defined so cargo never
# forwards an unresolved placeholder; empty values are skipped by ConfigurationPropertiesImpl,
# so stack.properties defaults and SSO credentials still apply. Never set stack.iam.* non-empty:
# it would shadow SSO.
for suffix in \
  stack.iam.id stack.iam.key stack.iam.session.token \
  doi.prefix doi.datacite.api.endpoint doi.datacite.username doi.datacite.password \
  google.cloud.key sts.iam.arn sts.duration.seconds \
  cloudfront.private.key.id cloudfront.domainname cloudfront.private.key.secret \
  docusign.private.key docusign.account.id docusign.user.id docusign.integration.key; do
  set_prop "org.sagebionetworks.${suffix}" ""
done

if [ -f "$SETTINGS_PROPERTIES" ]; then
  lineno=0
  while IFS= read -r line || [ -n "$line" ]; do
    lineno=$((lineno + 1))
    line="${line//$'\r'/}"
    stripped="$(trim "$line")"
    if [ -z "$stripped" ] || [ "${stripped:0:1}" = "#" ]; then
      continue
    fi
    key="$(trim "${line%%=*}")"
    if [[ "$line" != *=* ]] || ! [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9._-]*$ ]]; then
      echo "Invalid key in settings.properties line ${lineno}" >&2
      exit 1
    fi
    set_prop "$key" "$(trim "${line#*=}")"
  done < "$SETTINGS_PROPERTIES"
fi

{
  echo '<?xml version="1.0" encoding="UTF-8"?>'
  echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">'
  echo '  <profiles>'
  echo '    <profile>'
  echo '      <id>devcontainer</id>'
  echo '      <properties>'
  for key in "${KEYS[@]}"; do
    printf '        <%s>%s</%s>\n' "$key" "$(xml_escape "${PROPS[$key]}")" "$key"
  done
  echo '      </properties>'
  echo '    </profile>'
  echo '  </profiles>'
  echo '  <activeProfiles>'
  echo '    <activeProfile>devcontainer</activeProfile>'
  echo '  </activeProfiles>'
  echo '</settings>'
} > "$M2_SETTINGS"

# --- 8. AWS SSO config, only when absent (the user may have customised it).
if [ ! -f "$AWS_CONFIG" ]; then
  cat > "$AWS_CONFIG" <<AWSCFG
[sso-session synapse-dev]
sso_start_url = https://d-906769aa66.awsapps.com/start
sso_region = us-east-1
sso_registration_scopes = sso:account:access

[default]
sso_session = synapse-dev
sso_account_id = ${SSO_ACCT_NUM}
sso_role_name = Developer
region = us-east-1
AWSCFG
fi

# --- 9. Summary.
cat <<SUMMARY

Devcontainer ready.
  Developer name : ${DEV_NAME}
  Databases      : ${DB_MAIN}, ${DB_TABLES} (user ${DB_USER}) on mysql8:3306
  Maven settings : ${M2_SETTINGS}

Next steps:
  aws sso login --use-device-code
  mvn clean install -DskipTests

After changing DEVELOPER_NAME, re-run this script and 'mvn clean install' (stack.properties is filtered at build time).
SUMMARY
