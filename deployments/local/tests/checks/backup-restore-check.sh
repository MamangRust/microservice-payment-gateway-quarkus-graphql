#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BACKUP_FILE="${BACKUP_FILE:-}"
BACKUP_CREATED=false
RESTORE_DB="${RESTORE_DB:-PAYMENT_GATEWAY_RESTORE_CHECK}"
RESTORE_CREATED=false

if [[ ! "$RESTORE_DB" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  echo "RESTORE_DB must be a simple PostgreSQL identifier" >&2
  exit 2
fi
if [[ "${RESTORE_DB,,}" == "payment_gateway" ]]; then
  echo "RESTORE_DB must not be the primary PAYMENT_GATEWAY database" >&2
  exit 2
fi

if [[ -z "$BACKUP_FILE" ]]; then
  BACKUP_FILE="$(mktemp "$ROOT_DIR/deployments/local/payment-gateway-backup.XXXXXX.sql")"
  BACKUP_CREATED=true
fi
TEMP_BACKUP="$(mktemp "${BACKUP_FILE}.tmp.XXXXXX")"

cleanup() {
  if [[ "$RESTORE_CREATED" == "true" ]]; then
    "${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U DRAGON -d postgres \
      -c "DROP DATABASE IF EXISTS \"$RESTORE_DB\";" >/dev/null 2>&1 || true
  fi
  rm -f "$TEMP_BACKUP"
  if [[ "$BACKUP_CREATED" == "true" ]]; then
    rm -f "$BACKUP_FILE"
  fi
}
trap cleanup EXIT

if [[ "${ALLOW_BACKUP_RESTORE_CHECK:-false}" != "true" ]]; then
  echo 'Set ALLOW_BACKUP_RESTORE_CHECK=true to run the non-destructive restore check.' >&2
  exit 2
fi

"${COMPOSE[@]}" exec -T postgres pg_dump -U DRAGON -d PAYMENT_GATEWAY --format=plain \
  --no-owner --no-privileges > "$TEMP_BACKUP"
[[ -s "$TEMP_BACKUP" ]] || { echo "Backup is empty: $TEMP_BACKUP" >&2; exit 1; }
mv -f "$TEMP_BACKUP" "$BACKUP_FILE"
TEMP_BACKUP="$(mktemp "${BACKUP_FILE}.tmp.XXXXXX")"

"${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U DRAGON -d postgres \
  -c "DROP DATABASE IF EXISTS \"$RESTORE_DB\";" \
  -c "CREATE DATABASE \"$RESTORE_DB\";"
RESTORE_CREATED=true
cat "$BACKUP_FILE" | "${COMPOSE[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U DRAGON -d "$RESTORE_DB" >/dev/null

MIGRATION_COUNT="$("${COMPOSE[@]}" exec -T postgres psql -X -At -U DRAGON -d "$RESTORE_DB" \
  -c 'SELECT count(*) FROM flyway_schema_history;')"
[[ "$MIGRATION_COUNT" =~ ^[0-9]+$ && "$MIGRATION_COUNT" -gt 0 ]] || {
  echo "Restore verification failed; migration history count=$MIGRATION_COUNT" >&2
  exit 1
}

echo "Backup/restore verification passed without touching the primary database volume."
