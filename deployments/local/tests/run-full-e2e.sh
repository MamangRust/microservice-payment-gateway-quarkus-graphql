#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${BASE_URL:-http://localhost:5000}"
LOG_DIR="/tmp/payment-gateway-e2e-full"
rm -rf "$LOG_DIR"
mkdir -p "$LOG_DIR"

# ─── Environment (NO HTTP_PORT global — set per-service) ──────────────────────
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_USER="DRAGON"
export DB_PASS="DRAGON"
export DB_USERNAME="DRAGON"
export DB_PASSWORD="DRAGON"
export DB_NAME="PAYMENT_GATEWAY"
export KAFKA_BROKERS="localhost:9092"
export KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
export REDIS_HOSTS="redis://:dragon_knight@localhost:6379"
export REDIS_CLIENT_TYPE="standalone"
export REDIS_PASSWORD="dragon_knight"
export REDIS_CLUSTER_ENABLED="false"
export OTEL_ENDPOINT="localhost:4317"
export CHAOS_CONFIG_PATH="./chaos.yaml"
export APP_ENV="local"
export AUTH_HOST=localhost USER_HOST=localhost CARD_HOST=localhost
export MERCHANT_HOST=localhost ROLE_HOST=localhost SALDO_HOST=localhost
export TOPUP_HOST=localhost TRANSACTION_HOST=localhost TRANSFER_HOST=localhost
export WITHDRAW_HOST=localhost
export USER_GRPC_HOST=localhost USER_SERVICE_HOST=localhost USER_SERVICE_GRPC_PORT=9011
export ROLE_GRPC_HOST=localhost ROLE_SERVICE_HOST=localhost ROLE_SERVICE_GRPC_PORT=9006
export CARD_SERVICE_HOST=localhost CARD_SERVICE_GRPC_PORT=9004
export SALDO_SERVICE_HOST=localhost SALDO_SERVICE_GRPC_PORT=9007
export MERCHANT_SERVICE_HOST=localhost MERCHANT_SERVICE_GRPC_PORT=9005

JAVA_OPTS="-Xms256m -Xmx512m"

cleanup() {
    echo ""
    echo "🧹 Cleaning up Java services..."
    for pid_file in "$LOG_DIR"/*.pid; do
        [ -f "$pid_file" ] || continue
        kill "$(cat "$pid_file")" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    echo "Done."
}
trap cleanup EXIT

# ─── Kill any existing Java services ──────────────────────────────────────────
echo "🔪 Killing any existing Java services..."
ps aux | grep java | grep -v grep | grep -E 'target/quarkus|quarkus-run' | awk '{print $2}' | xargs kill -9 2>/dev/null || true
sleep 3
# Also kill by port
for port in 5000 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094; do
    if timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null; then
        fuser -k ${port}/tcp 2>/dev/null || true
    fi
done
sleep 2
echo "Ports verified."

# ─── Flush Redis cache (stale entries from previous runs) ──────────────────────
echo "🗑️  Flushing Redis cache..."
docker exec e2e-redis-standalone redis-cli -a dragon_knight --no-auth-warning FLUSHALL >/dev/null 2>&1
echo "  ✅ Redis flushed"

# ─── Seed DB BEFORE starting services ─────────────────────────────────────────
echo ""
echo "🌱 Seeding database..."
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "
INSERT INTO payment_identity.roles (role_name, created_at, updated_at)
VALUES ('ROLE_ADMIN', now(), now()), ('ROLE_USER', now(), now()),
       ('ROLE_CASHIER', now(), now()), ('ROLE_MERCHANT', now(), now())
ON CONFLICT DO NOTHING;
" >/dev/null 2>&1
echo "  ✅ Roles seeded (ROLE_ADMIN, ROLE_USER, ROLE_CASHIER, ROLE_MERCHANT)"

# ─── Start services in waves ──────────────────────────────────────────────────
echo ""
echo "🚀 Starting Java services..."

echo "  Wave 1: user + transaction..."
java $JAVA_OPTS -jar user/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/user.log" 2>&1 &
echo "$!" > "$LOG_DIR/user.pid"
java $JAVA_OPTS -jar transaction/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/transaction.log" 2>&1 &
echo "$!" > "$LOG_DIR/transaction.pid"
sleep 25
for port in 8091 9011 8089 9009; do
    timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null && echo "  ✅ $port" || echo "  ❌ $port"
done

echo "  Wave 2a: auth + role + card..."
java $JAVA_OPTS -jar auth/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/auth.log" 2>&1 &
echo "$!" > "$LOG_DIR/auth.pid"
java $JAVA_OPTS -jar role/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/role.log" 2>&1 &
echo "$!" > "$LOG_DIR/role.pid"
java $JAVA_OPTS -jar card/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/card.log" 2>&1 &
echo "$!" > "$LOG_DIR/card.pid"
sleep 30
for port in 8092 9012 8086 9006 8084 9004; do
    timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null && echo "  ✅ $port" || echo "  ❌ $port"
done

echo "  Wave 2b: merchant + saldo + topup + transfer + withdraw..."
java $JAVA_OPTS -jar merchant/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/merchant.log" 2>&1 &
echo "$!" > "$LOG_DIR/merchant.pid"
java $JAVA_OPTS -jar saldo/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/saldo.log" 2>&1 &
echo "$!" > "$LOG_DIR/saldo.pid"
java $JAVA_OPTS -jar topup/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/topup.log" 2>&1 &
echo "$!" > "$LOG_DIR/topup.pid"
java $JAVA_OPTS -jar transfer/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/transfer.log" 2>&1 &
echo "$!" > "$LOG_DIR/transfer.pid"
java $JAVA_OPTS -jar withdraw/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/withdraw.log" 2>&1 &
echo "$!" > "$LOG_DIR/withdraw.pid"
sleep 35
for port in 8085 9005 8087 9007 8088 9008 8090 9010 8093 9013; do
    timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" 2>/dev/null && echo "  ✅ $port" || echo "  ❌ $port"
done

echo "  Wave 3: gateway + email + stats..."
HTTP_PORT=5000 java $JAVA_OPTS -jar gateway/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/gateway.log" 2>&1 &
echo "$!" > "$LOG_DIR/gateway.pid"
HTTP_PORT=8094 java $JAVA_OPTS -jar email-service/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/email-service.log" 2>&1 &
echo "$!" > "$LOG_DIR/email-service.pid"
java $JAVA_OPTS -jar stats-writer/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/stats-writer.log" 2>&1 &
echo "$!" > "$LOG_DIR/stats-writer.pid"
sleep 15
java $JAVA_OPTS -jar stats-reader/target/quarkus-app/quarkus-run.jar >"$LOG_DIR/stats-reader.log" 2>&1 &
echo "$!" > "$LOG_DIR/stats-reader.pid"
sleep 15

# Pre-warm Kafka topic by sending a message directly
echo "  Pre-warming Kafka topic..."
echo '{"test":true}' | docker exec -i my-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic email-service-topic-auth-register 2>/dev/null || true
sleep 2
# Cleanup any leftover test users from previous runs
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "DELETE FROM payment_identity.user_roles WHERE user_id IN (SELECT id FROM payment_identity.users WHERE email LIKE '%@example.test');" >/dev/null 2>&1 || true
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "DELETE FROM payment_identity.users WHERE email LIKE '%@example.test';" >/dev/null 2>&1 || true
echo "  ✅ Cleanup done"

# Verify gateway
echo "  Checking gateway health..."
for _ in $(seq 1 20); do
    curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null 2>&1 && break
    sleep 3
done
if curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null 2>&1; then
    echo "  ✅ Gateway ready at $BASE_URL"
else
    echo "  ❌ Gateway NOT ready! Aborting."
    tail -30 "$LOG_DIR/gateway.log" 2>/dev/null
    exit 1
fi

# ─── Run all 4 hurl E2E tests ────────────────────────────────────────────────
PASS=0
FAIL=0

run_hurl_test() {
    local name="$1"
    local hurl_file="$2"
    local email_var="$3"
    local merchant_var="$4"

    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Running: $name"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    local EMAIL
    EMAIL="$(date -u +%Y%m%d%H%M%S)-$$-${name}@example.test"

    # Register user
    local REGISTER_BODY
    REGISTER_BODY="/tmp/payment-gateway-${name}-register.json"
    rm -f "$REGISTER_BODY"
    # Verify gateway is still alive
    if ! curl --silent --fail --max-time 3 "$BASE_URL/q/health" >/dev/null 2>&1; then
        echo "  ❌ Gateway is DOWN! Skipping $name"
        FAIL=$((FAIL+1))
        return 1
    fi
    local REGISTER_STATUS
    REGISTER_STATUS=$(curl --silent --show-error --max-time 120 -o "$REGISTER_BODY" -w '%{http_code}' \
        -H 'Content-Type: application/json' \
        -d "{\"firstname\":\"${name}\",\"lastname\":\"Runner\",\"email\":\"${EMAIL}\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" \
        "$BASE_URL/api/auth/register" 2>/dev/null)
    if [ -z "$REGISTER_STATUS" ]; then
        REGISTER_STATUS="000"
    fi
    echo "  Register HTTP status: $REGISTER_STATUS"

    if [[ "$REGISTER_STATUS" != "201" ]]; then
        echo "  ❌ Registration failed (HTTP $REGISTER_STATUS)"
        echo "  Response body:"
        cat "$REGISTER_BODY" 2>/dev/null || echo "  (empty)"
        echo ""
        FAIL=$((FAIL+1))
        return 1
    fi

    local USER_ID
    USER_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from users where email='${EMAIL}';" 2>/dev/null)"
    if [[ -z "$USER_ID" ]]; then
        echo "  ❌ Could not find user_id for $EMAIL"
        FAIL=$((FAIL+1))
        return 1
    fi

    local ADMIN_ROLE_ID
    ADMIN_ROLE_ID="$(docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -Atc "select id from roles where role_name='ROLE_ADMIN' and deleted_at is null;" 2>/dev/null)"
    docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "insert into user_roles(user_id,role_id) values (${USER_ID},${ADMIN_ROLE_ID}) on conflict do nothing;" >/dev/null 2>&1

    echo "  User: $EMAIL (id=$USER_ID)"

    local VARS="--variable base_url=$BASE_URL --variable user_id=$USER_ID"

    if [[ -n "$email_var" ]]; then
        VARS="$VARS --variable ${email_var}=${EMAIL}"
    fi
    if [[ -n "$merchant_var" ]]; then
        local MERCHANT_NAME="${merchant_var} ${EMAIL}"
        VARS="$VARS --variable $merchant_var=$MERCHANT_NAME"
    fi

    # Use variables-file to avoid @ in email being interpreted as file ref by hurl
    local VARS_FILE
    VARS_FILE=$(mktemp /tmp/hurl-vars-XXXXXX)
    echo "base_url=$BASE_URL" > "$VARS_FILE"
    echo "user_id=$USER_ID" >> "$VARS_FILE"
    [[ -n "$email_var" ]] && echo "${email_var}=${EMAIL}" >> "$VARS_FILE"
    [[ -n "$merchant_var" ]] && echo "${merchant_var}=${MERCHANT_NAME}" >> "$VARS_FILE"
    if hurl --test --variables-file "$VARS_FILE" "$hurl_file" 2>&1; then
        echo "  ✅ $name PASSED"
        PASS=$((PASS+1))
    else
        echo "  ❌ $name FAILED"
        FAIL=$((FAIL+1))
    fi    # Cleanup user
docker exec postgres psql -U DRAGON -d PAYMENT_GATEWAY -c "delete from users where email='${EMAIL}';" >/dev/null 2>&1 || true
    rm -f "$VARS_FILE"
}

run_hurl_test "e2e" "$ROOT_DIR/deployments/local/tests/e2e.hurl" "e2e_email" "e2e_merchant" || true
run_hurl_test "credit-lifecycle" "$ROOT_DIR/deployments/local/tests/credit-lifecycle.hurl" "e2e_email" "" || true
run_hurl_test "stats" "$ROOT_DIR/deployments/local/tests/stats.hurl" "stats_email" "stats_merchant" || true
run_hurl_test "fraud-scoring" "$ROOT_DIR/deployments/local/tests/fraud-scoring.hurl" "e2e_email" "" || true

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
