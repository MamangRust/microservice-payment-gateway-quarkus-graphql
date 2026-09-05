#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${HOST_ENV_FILE:-$ROOT_DIR/deployments/local/host.env}"
LOG_DIR="${HOST_JAVA_LOG_DIR:-/tmp/payment-gateway-host-logs}"
PID_FILE="${HOST_JAVA_PID_FILE:-/tmp/payment-gateway-host-supervisor.pid}"

cd "$ROOT_DIR"
[[ -r "$ENV_FILE" ]] || { echo "Missing host environment file: $ENV_FILE" >&2; exit 1; }
# shellcheck disable=SC1090
source "$ENV_FILE"

stop_existing() {
    if [[ -f "$PID_FILE" ]]; then
        local supervisor_pid
        supervisor_pid="$(cat "$PID_FILE")"
        # The supervisor is a session/process-group leader created by setsid.
        # Kill the group so child Java services cannot survive a restart.
        kill -- -"$supervisor_pid" 2>/dev/null || kill "$supervisor_pid" 2>/dev/null || true
        for _ in $(seq 1 20); do
            kill -0 "$supervisor_pid" 2>/dev/null || break
            sleep 1
        done
    fi
    for pid_file in "$LOG_DIR"/*.pid; do
        [[ -f "$pid_file" ]] || continue
        kill "$(cat "$pid_file")" 2>/dev/null || true
    done
}

if [[ "${1:-}" == "stop" ]]; then
    stop_existing
    echo "Host Java services stopped."
    exit 0
fi

mkdir -p "$LOG_DIR"
stop_existing
rm -f "$LOG_DIR"/*.log "$LOG_DIR"/*.pid "$PID_FILE"

export DB_HOST DB_PORT DB_USER DB_PASS DB_USERNAME DB_PASSWORD DB_NAME
export KAFKA_BROKERS KAFKA_BOOTSTRAP_SERVERS REDIS_HOSTS REDIS_PASSWORD REDIS_CLUSTER_ENABLED REDIS_CLIENT_TYPE
export OTEL_ENDPOINT CHAOS_CONFIG_PATH APP_ENV

# F1: per-service migrations. The user service owns the payment_identity schema
# (incl. role-level search_path bootstrap); the transaction service owns
# payment_finance. Both must be up before services that validate against those
# tables or create cross-schema FKs (card/merchant -> identity, finance readers
# -> payment_finance).
java -jar "$ROOT_DIR/user/target/quarkus-app/quarkus-run.jar" >"$LOG_DIR/user.log" 2>&1 &
echo "$!" >"$LOG_DIR/user.pid"
java -jar "$ROOT_DIR/transaction/target/quarkus-app/quarkus-run.jar" >"$LOG_DIR/transaction.log" 2>&1 &
echo "$!" >"$LOG_DIR/transaction.pid"
# Give identity + finance schemas (and Flyway) a head start before the rest boot.
sleep 12

setsid bash -c '
set -euo pipefail
ROOT="$1"
LOG_DIR="$2"

start_service() {
    local name="$1" jar="$2"
    shift 2
    ( export "$@"; exec java -jar "$ROOT/$jar" ) >"$LOG_DIR/$name.log" 2>&1 &
    echo "$!" >"$LOG_DIR/$name.pid"
}

start_service gateway gateway/target/quarkus-app/quarkus-run.jar \
    HTTP_PORT=5000 AUTH_HOST=localhost USER_HOST=localhost CARD_HOST=localhost \
    MERCHANT_HOST=localhost ROLE_HOST=localhost SALDO_HOST=localhost TOPUP_HOST=localhost \
    TRANSACTION_HOST=localhost TRANSFER_HOST=localhost WITHDRAW_HOST=localhost
start_service auth auth/target/quarkus-app/quarkus-run.jar \
    USER_GRPC_HOST=localhost ROLE_GRPC_HOST=localhost GRPC_PORT=9012
start_service card card/target/quarkus-app/quarkus-run.jar USER_SERVICE_HOST=localhost
start_service merchant merchant/target/quarkus-app/quarkus-run.jar USER_SERVICE_HOST=localhost
start_service role role/target/quarkus-app/quarkus-run.jar
start_service saldo saldo/target/quarkus-app/quarkus-run.jar CARD_SERVICE_HOST=localhost
start_service topup topup/target/quarkus-app/quarkus-run.jar CARD_SERVICE_HOST=localhost SALDO_SERVICE_HOST=localhost
start_service transfer transfer/target/quarkus-app/quarkus-run.jar \
    CARD_SERVICE_HOST=localhost SALDO_SERVICE_HOST=localhost
start_service withdraw withdraw/target/quarkus-app/quarkus-run.jar \
    CARD_SERVICE_HOST=localhost SALDO_SERVICE_HOST=localhost
start_service email-service email-service/target/quarkus-app/quarkus-run.jar

exec sleep infinity
' host-java-supervisor "$ROOT_DIR" "$LOG_DIR" >"$LOG_DIR/supervisor.log" 2>&1 &

supervisor_pid=$!
echo "$supervisor_pid" >"$PID_FILE"
disown "$supervisor_pid" 2>/dev/null || true

for _ in $(seq 1 "${HOST_JAVA_WAIT_ATTEMPTS:-120}"); do
    ready=true
    curl --silent --fail --max-time 2 http://localhost:5000/q/health/ready >/dev/null 2>&1 || ready=false
    for port in 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 9004 9005 9006 9007 9008 9009 9010 9011 9012 9013; do
        timeout 1 bash -c "</dev/tcp/127.0.0.1/$port" >/dev/null 2>&1 || ready=false
    done
    [[ "$ready" == true ]] && { echo "Host Java services ready (gateway: http://localhost:5000)."; exit 0; }
    sleep 2
done

echo "Host Java services did not become ready; see $LOG_DIR" >&2
exit 1
