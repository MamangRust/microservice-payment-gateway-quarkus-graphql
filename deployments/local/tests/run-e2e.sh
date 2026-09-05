#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-e2e-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"
E2E_ROLE="E2E_ROLE_e2e-$(date -u +%Y%m%d%H%M%S)-$$"
E2E_MERCHANT="E2E Merchant ${E2E_EMAIL}"

wait_for_gateway() {
  for _ in $(seq 1 "${E2E_WAIT_ATTEMPTS:-60}"); do
    curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null && return 0
    sleep 2
  done
  echo "Gateway did not become ready: $BASE_URL/q/health/ready" >&2
  "${COMPOSE[@]}" ps >&2 || true
  exit 1
}

cleanup() {
  local id
  id="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$E2E_EMAIL';" 2>/dev/null || true)"
  if [[ -n "$id" ]]; then
    docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "delete from users where id=$id;" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

wait_for_gateway
REGISTER_BODY=/tmp/payment-gateway-e2e-register.json
REGISTER_STATUS="$(curl --silent --show-error --max-time 15 -o "$REGISTER_BODY" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"E2E\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" \
  "$BASE_URL/api/auth/register")"
[[ "$REGISTER_STATUS" == 201 ]] || { cat "$REGISTER_BODY" >&2; exit 1; }
USER_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$E2E_EMAIL';")"
ADMIN_ROLE_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN' and deleted_at is null;")"
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values ($USER_ID,$ADMIN_ROLE_ID) on conflict do nothing;" >/dev/null

hurl --test \
  --variable base_url="$BASE_URL" \
  --variable e2e_email="$E2E_EMAIL" \
  --variable user_id="$USER_ID" \
  --variable e2e_role="$E2E_ROLE" \
  --variable e2e_merchant="$E2E_MERCHANT" \
  "$ROOT_DIR/deployments/local/tests/e2e.hurl"

echo "REST E2E passed for $E2E_EMAIL"
