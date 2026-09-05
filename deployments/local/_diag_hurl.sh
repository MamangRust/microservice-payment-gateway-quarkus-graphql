#!/usr/bin/env bash
# Temporary: run the real Hurl suites with --verbose against a fresh user to capture 400 bodies.
set +e
BASE=http://localhost:5000
EMAIL="hv.$(date +%s).$$@example.test"
PASS="E2E-password-123"

curl -sS -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  --data "{\"firstname\":\"Hv\",\"lastname\":\"Run\",\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"confirmPassword\":\"$PASS\"}" >/dev/null
USER_ID=$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$EMAIL';" 2>/dev/null)
ROLE_ID=$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN' and deleted_at is null;" 2>/dev/null)
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values ($USER_ID,$ROLE_ID) on conflict do nothing;" >/dev/null 2>&1
echo "USER_ID=$USER_ID"

echo "=========== STATS SUITE (verbose) ==========="
hurl --verbose --variable base_url="$BASE" --variable stats_email="$EMAIL" \
  --variable user_id="$USER_ID" --variable stats_merchant="Hv Stats Merchant" \
  deployments/local/tests/stats.hurl >/tmp/hurl-stats-verbose.out 2>&1
echo "STATS_EXIT=$?"
grep -n -B3 -A18 'HTTP 400\|error: Assert' /tmp/hurl-stats-verbose.out | head -120
echo "--- raw 400 bodies ---"
grep -n -A12 'HTTP/1.1 400' /tmp/hurl-stats-verbose.out | head -80

echo "=========== E2E SUITE (verbose) ==========="
EMAIL2="hv2.$(date +%s).$$@example.test"
curl -sS -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  --data "{\"firstname\":\"Hv\",\"lastname\":\"E2e\",\"email\":\"$EMAIL2\",\"password\":\"$PASS\",\"confirmPassword\":\"$PASS\"}" >/dev/null
USER_ID2=$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$EMAIL2';" 2>/dev/null)
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values ($USER_ID2,$ROLE_ID) on conflict do nothing;" >/dev/null 2>&1
hurl --verbose --variable base_url="$BASE" --variable e2e_email="$EMAIL2" \
  --variable user_id="$USER_ID2" --variable e2e_role="HvRole" \
  --variable e2e_merchant="Hv E2E Merchant" \
  deployments/local/tests/e2e.hurl >/tmp/hurl-e2e-verbose.out 2>&1
echo "E2E_EXIT=$?"
echo "--- raw 400 bodies ---"
grep -n -A12 'HTTP/1.1 400' /tmp/hurl-e2e-verbose.out | head -100
