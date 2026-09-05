#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:5000}"

COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")

PASS=0
FAIL=0

run_hurl_test() {
  local name="$1"
  local hurl_file="$2"
  local email_var="$3"
  local merchant_var="$4"

  echo ""
  echo "============================================"
  echo " Running: $name"
  echo "============================================"

  local EMAIL
  EMAIL="$(date -u +%Y%m%d%H%M%S)-$$-${name}@example.test"

  # Register user
  local REGISTER_BODY
  REGISTER_BODY=/tmp/payment-gateway-${name}-register.json
  local REGISTER_STATUS
  REGISTER_STATUS="$(curl --silent --show-error --max-time 15 -o "$REGISTER_BODY" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -d "{\"firstname\":\"${name}\",\"lastname\":\"Runner\",\"email\":\"${EMAIL}\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" \
    "$BASE_URL/api/auth/register" 2>/dev/null || echo "000")"

  if [[ "$REGISTER_STATUS" != "201" ]]; then
    echo "❌ Registration failed ($REGISTER_STATUS)"
    cat "$REGISTER_BODY" 2>/dev/null
    FAIL=$((FAIL+1))
    return 1
  fi

  local USER_ID
  USER_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='${EMAIL}';" 2>/dev/null)"
  if [[ -z "$USER_ID" ]]; then
    echo "❌ Could not find user_id for $EMAIL"
    FAIL=$((FAIL+1))
    return 1
  fi

  local ADMIN_ROLE_ID
  ADMIN_ROLE_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN' and deleted_at is null;" 2>/dev/null)"
  docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values (${USER_ID},${ADMIN_ROLE_ID}) on conflict do nothing;" >/dev/null 2>&1

  echo "  User: $EMAIL (id=$USER_ID)"

  local VARS="--variable base_url=$BASE_URL --variable user_id=$USER_ID"

  if [[ -n "$email_var" ]]; then
    VARS="$VARS --variable $email_var=$EMAIL"
  fi
  if [[ -n "$merchant_var" ]]; then
    local MERCHANT_NAME="${merchant_var} ${EMAIL}"
    VARS="$VARS --variable $merchant_var=$MERCHANT_NAME"
  fi

  if hurl --test $VARS "$hurl_file" 2>&1; then
    echo "✅ $name PASSED"
    PASS=$((PASS+1))
  else
    echo "❌ $name FAILED"
    FAIL=$((FAIL+1))
  fi

  # Cleanup
  docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "delete from users where email='${EMAIL}';" >/dev/null 2>&1 || true
}

# Wait for gateway
echo "Waiting for gateway at $BASE_URL..."
for _ in $(seq 1 60); do
  curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null 2>&1 && break
  sleep 2
done
curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null 2>&1 || { echo "Gateway not ready!"; exit 1; }
echo "Gateway ready!"

# Run all 4 tests
run_hurl_test "e2e" "$ROOT_DIR/deployments/local/tests/e2e.hurl" "e2e_email" "e2e_merchant"
run_hurl_test "credit-lifecycle" "$ROOT_DIR/deployments/local/tests/credit-lifecycle.hurl" "e2e_email" ""
run_hurl_test "stats" "$ROOT_DIR/deployments/local/tests/stats.hurl" "stats_email" "stats_merchant"
run_hurl_test "fraud-scoring" "$ROOT_DIR/deployments/local/tests/fraud-scoring.hurl" "e2e_email" ""

echo ""
echo "============================================"
echo " E2E Results Summary"
echo "============================================"
echo "  Total:  $((PASS + FAIL))"
echo "  Passed: $PASS"
echo "  Failed: $FAIL"
echo ""

if [[ "$FAIL" -eq 0 ]]; then
  echo "🎉 ALL TESTS PASSED!"
else
  echo "⚠️  $FAIL test(s) failed"
fi
