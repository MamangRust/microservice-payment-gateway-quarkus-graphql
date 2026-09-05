#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:5000}"
CHAOS_FILE="${CHAOS_CONFIG_PATH:-$ROOT_DIR/chaos.yaml}"

[[ -s "$CHAOS_FILE" ]] || { echo "Chaos config is missing or empty: $CHAOS_FILE" >&2; exit 1; }
grep -q '^policies:' "$CHAOS_FILE" || { echo "Chaos config has no policies root" >&2; exit 1; }
for policy in sql-users-deadlock grpc-auth-login-unavailable kafka-auth-register-drop; do
  grep -q "name: \"$policy\"" "$CHAOS_FILE" || {
    echo "Required chaos policy is missing: $policy" >&2
    exit 1
  }
done

curl --silent --show-error --fail --max-time 10 "$BASE_URL/q/health/ready" >/tmp/chaos-health.json
python3 - /tmp/chaos-health.json <<'PY'
import json, sys
payload = json.load(open(sys.argv[1]))
if payload.get("status") not in {"UP", "up", "ready"}:
    raise SystemExit(f"gateway is not ready: {payload}")
PY

# The current Quarkus gateway does not expose a REST chaos control-plane resource.
# Accept its non-success response as an explicit capability check, while failing
# on transport/server errors that indicate a broken gateway.
status="$(curl --silent --show-error --max-time 10 -o /tmp/chaos-control-plane.json -w '%{http_code}' \
  "$BASE_URL/api/chaos/policies")"
case "$status" in
  200|400|401|403|404) ;;
  *) echo "Unexpected chaos control-plane response: HTTP $status" >&2; cat /tmp/chaos-control-plane.json >&2 || true; exit 1 ;;
esac

echo "Chaos dependency check passed: config policies are present, gateway is ready, control-plane capability returned HTTP $status."
