#!/usr/bin/env bash
# Temporary diagnostic: reproduce remaining failing endpoints with full bodies.
set +e
BASE=http://localhost:5000
EMAIL="diag2.$(date +%s).$$@example.test"
PASS="E2E-password-123"

echo "=== 1. REGISTER ==="
curl -sS -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  --data "{\"firstname\":\"Diag\",\"lastname\":\"X\",\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"confirmPassword\":\"$PASS\"}"
echo

TEST_UID=$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='$EMAIL';" 2>/dev/null)
ADMIN_RID=$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN';" 2>/dev/null)
echo "TEST_UID=$TEST_UID"
[ -z "$TEST_UID" ] && { echo "NO_USER"; exit 1; }
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values($TEST_UID,$ADMIN_RID) on conflict do nothing" >/dev/null 2>&1

echo "=== 2. LOGIN ==="
LOGIN=$(curl -sS -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  --data "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}")
TOKEN=$(printf '%s' "$LOGIN" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["accessToken"])' 2>/dev/null)
[ -z "$TOKEN" ] && { echo "NO_TOKEN"; exit 1; }
echo "TOKEN ok"

CARD=$(curl -sS -X POST "$BASE/api/cards" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"userId\":$TEST_UID,\"cardType\":\"VISA\",\"expireDate\":\"2035-12-31\",\"cvv\":\"123\",\"cardProvider\":\"BCA\"}")
CARD_NUMBER=$(printf '%s' "$CARD" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["cardNumber"])' 2>/dev/null)
echo "CARD_NUMBER=$CARD_NUMBER"

SALDO=$(curl -sS -X POST "$BASE/api/saldos" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"cardNumber\":\"$CARD_NUMBER\",\"totalBalance\":1000000}")
SALDO_ID=$(printf '%s' "$SALDO" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["saldoId"])' 2>/dev/null)
curl -sS -X PUT "$BASE/api/saldos/$SALDO_ID" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"cardNumber\":\"$CARD_NUMBER\",\"totalBalance\":1000000}" >/dev/null

MER=$(curl -sS -X POST "$BASE/api/merchants" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"name\":\"Diag2 Merchant $TEST_UID\",\"userId\":$TEST_UID}")
MER_ID=$(printf '%s' "$MER" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["id"])' 2>/dev/null)
API_KEY=$(printf '%s' "$MER" | python3 -c 'import json,sys;print(json.load(sys.stdin)["data"]["apiKey"])' 2>/dev/null)
echo "MER_ID=$MER_ID"

echo "=== 3. MERCHANT-DOCUMENT CREATE ==="
curl -sS -i -X POST "$BASE/api/merchant-documents" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"merchantId\":$MER_ID,\"documentType\":\"BUSINESS_LICENSE\",\"documentUrl\":\"https://example.test/$TEST_UID.pdf\"}"
echo

echo "=== 4. TOPUP POST 200000 ==="
curl -sS -i -X POST "$BASE/api/topups" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"cardNumber\":\"$CARD_NUMBER\",\"topupAmount\":200000,\"topupMethod\":\"BANK_TRANSFER\",\"idempotencyKey\":\"diag2-topup-$TEST_UID\"}"
echo

echo "=== 5. TRANSACTION POST 50000 (e2e value) ==="
curl -sS -i -X POST "$BASE/api/transactions" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"apiKey\":\"$API_KEY\",\"cardNumber\":\"$CARD_NUMBER\",\"amount\":50000,\"paymentMethod\":\"BANK_TRANSFER\",\"merchantId\":$MER_ID,\"idempotencyKey\":\"diag2-txn-$TEST_UID\"}"
echo

echo "=== 6. WITHDRAW POST 60000 (stats value) ==="
curl -sS -i -X POST "$BASE/api/withdraws" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  --data "{\"cardNumber\":\"$CARD_NUMBER\",\"withdrawAmount\":60000,\"idempotencyKey\":\"diag2-wd-$TEST_UID\"}"
echo

echo "=== 7. SALDO after operations (check balance flow) ==="
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select saldo_id,total_balance,withdraw_amount,withdraw_time from saldos where card_number='$CARD_NUMBER';" 2>/dev/null

echo "=== 8. GET transactions by id (fake id to test session) ==="
curl -sS -i -X GET "$BASE/api/transactions/999999" -H "Authorization: Bearer $TOKEN"
echo

echo "=== 9. GET cards/stats/topup/monthly/by-card ==="
curl -sS -i -X GET "$BASE/api/cards/stats/topup/monthly/by-card?year=2035&cardNumber=$CARD_NUMBER" -H "Authorization: Bearer $TOKEN"
echo

echo "=== 10. SERVICE LOGS ==="
for s in transaction withdraw; do
  echo "--- $s ---"
  docker logs --since 4m "$s" 2>&1 | grep -Ei 'ERROR|Exception|Caused|timeout|reject|validation|balance|Insufficient' | tail -20
done
