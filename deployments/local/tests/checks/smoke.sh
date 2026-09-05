#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:5000}"
EXPECT_CHAOS="${EXPECT_CHAOS:-false}"
AUTH_HEADER="${AUTH_HEADER:-}"
CURL=(curl --silent --show-error --max-time "${SMOKE_TIMEOUT_SECONDS:-10}")

request_status() {
  local method="${1:-GET}"
  local url="$2"
  if [[ -n "$AUTH_HEADER" ]]; then
    "${CURL[@]}" -X "$method" -H "$AUTH_HEADER" -o /tmp/payment-gateway-smoke-body -w '%{http_code}' "$url"
  else
    "${CURL[@]}" -X "$method" -o /tmp/payment-gateway-smoke-body -w '%{http_code}' "$url"
  fi
}

assert_status() {
  local expected="$1"
  local method="${2:-GET}"
  local url="$3"
  local actual
  actual="$(request_status "$method" "$url")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Expected HTTP $expected from $method $url, got $actual" >&2
    cat /tmp/payment-gateway-smoke-body >&2 || true
    exit 1
  fi
  echo "OK $actual $method $url"
}

assert_one_of() {
  local method="$1"
  local url="$2"
  shift 2
  local actual
  actual="$(request_status "$method" "$url")"
  for expected in "$@"; do
    [[ "$actual" == "$expected" ]] && { echo "OK $actual $method $url"; return 0; }
  done
  echo "Expected one of [$*] from $method $url, got $actual" >&2
  cat /tmp/payment-gateway-smoke-body >&2 || true
  exit 1
}

# Quarkus SmallRye Health is exposed under /q/health through Nginx/gateway.
assert_status 200 GET "$BASE_URL/q/health"
assert_status 200 GET "$BASE_URL/q/health/live"
assert_status 200 GET "$BASE_URL/q/health/ready"

if [[ "$EXPECT_CHAOS" == "true" ]]; then
  if [[ -z "$AUTH_HEADER" ]]; then
    echo 'EXPECT_CHAOS=true requires AUTH_HEADER="Authorization: Bearer <admin-token>"' >&2
    exit 2
  fi
  assert_status 200 GET "$BASE_URL/api/chaos/policies"
else
  # The current build has no REST chaos resource. Depending on whether the
  # request is handled by the gateway or rejected by its security layer, the
  # observed capability response is 400 or 404; both mean disabled/unexposed.
  assert_one_of GET "$BASE_URL/api/chaos/policies" 400 404
  assert_one_of POST "$BASE_URL/api/chaos/policies/reload" 400 404
  assert_one_of POST "$BASE_URL/api/chaos/halt" 400 404
fi

echo "Payment Gateway smoke test passed (BASE_URL=$BASE_URL, EXPECT_CHAOS=$EXPECT_CHAOS)"
