#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-clc-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"

wait_for_gateway() { for _ in $(seq 1 "${E2E_WAIT_ATTEMPTS:-60}"); do curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null && return 0; sleep 2; done; exit 1; }
cleanup() { local id; id="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$E2E_EMAIL';" 2>/dev/null || true)"; [[ -z "$id" ]] || docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "delete from users where id=$id;" >/dev/null 2>&1 || true; }
trap cleanup EXIT
wait_for_gateway
BODY=/tmp/payment-gateway-clc-register.json
STATUS="$(curl --silent --show-error --max-time 15 -o "$BODY" -w '%{http_code}' -H 'Content-Type: application/json' -d "{\"firstname\":\"CLC\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" "$BASE_URL/api/auth/register")"
[[ "$STATUS" == 201 ]] || { cat "$BODY" >&2; exit 1; }
USER_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$E2E_EMAIL';")"
ROLE_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN' and deleted_at is null;")"
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values ($USER_ID,$ROLE_ID) on conflict do nothing;" >/dev/null
hurl --test --variable base_url="$BASE_URL" --variable e2e_email="$E2E_EMAIL" --variable user_id="$USER_ID" "$ROOT_DIR/deployments/local/tests/credit-lifecycle.hurl"
echo "Credit lifecycle smoke passed for $E2E_EMAIL"
