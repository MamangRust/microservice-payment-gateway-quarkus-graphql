#!/usr/bin/env bash
set -euo pipefail

# Build all Java service images in configurable batches (default 2 images per
# batch, built in parallel) to avoid exhausting resources on large builds.
#
# Usage:
#   deployments/local/build-image.sh                     # build all (batch of 2)
#   deployments/local/build-image.sh auth card           # build selected services
#   BATCH_SIZE=4 TAG=1.0.0 deployments/local/build-image.sh
#   PUSH=true deployments/local/build-image.sh
#   deployments/local/build-image.sh --dry-run           # only print the batches

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deployments/local/docker-compose.yml"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$COMPOSE_FILE")

BATCH_SIZE="${BATCH_SIZE:-2}"
TAG="${TAG:-latest}"
IMAGE_PREFIX="${IMAGE_PREFIX:-monolith-payment-gateway-grpc-quarkus}"
PUSH="${PUSH:-false}"
PULL="${PULL:-false}"
NO_CACHE="${NO_CACHE:-false}"
DRY_RUN=false

# Fallback used only when docker compose --format json or python3 is unavailable.
FALLBACK_SERVICES=(
    gateway auth merchant card saldo role
    topup transaction transfer user email-service withdraw seeder
    stats-writer stats-reader
)
FALLBACK_DOCKERFILE() { echo "$1/Dockerfile"; }

for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=true ;;
        --push) PUSH=true ;;
        --no-cache) NO_CACHE=true ;;
        --pull) PULL=true ;;
        -*) echo "Unknown option: $arg" >&2; exit 2 ;;
        *) SELECTED_SERVICES+=("$arg") ;;
    esac
done

[[ "$BATCH_SIZE" =~ ^[0-9]+$ && "$BATCH_SIZE" -ge 1 ]] || { echo "BATCH_SIZE must be a positive integer" >&2; exit 2; }
if [[ "$PUSH" == "true" ]] && ! command -v docker buildx >/dev/null 2>&1; then
    echo "PUSH=true requires docker buildx (docker build fallback cannot push)." >&2
    exit 2
fi

# Parse the compose services once: buildable service -> Dockerfile path.
declare -A SERVICE_DOCKERFILE
compose_json="$("${COMPOSE[@]}" config --format json 2>/dev/null || true)"
if [[ -n "$compose_json" ]] && command -v python3 >/dev/null 2>&1; then
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        svc="${line%%|*}"
        dockerfile="${line#*|}"
        SERVICE_DOCKERFILE["$svc"]="$dockerfile"
    done < <(python3 - "$compose_json" <<'PY'
import json, sys
try:
    data = json.loads(sys.argv[1])
except Exception:
    sys.exit(1)
for name, svc in data.get('services', {}).items():
    build = svc.get('build') or {}
    if isinstance(build, dict) and build.get('context'):
        print(name + '|' + (build.get('dockerfile') or ''))
PY
)
fi

if [[ ${#SERVICE_DOCKERFILE[@]} -eq 0 ]]; then
    echo "Warning: could not parse compose build services; using fallback service list." >&2
    for svc in "${FALLBACK_SERVICES[@]}"; do
        SERVICE_DOCKERFILE["$svc"]="$(FALLBACK_DOCKERFILE "$svc")"
    done
fi

if [[ -n "${SELECTED_SERVICES[*]:-}" ]]; then
    BUILD_SERVICES=("${SELECTED_SERVICES[@]}")
else
    BUILD_SERVICES=()
    for svc in $("${COMPOSE[@]}" config --services); do
        [[ -n "${SERVICE_DOCKERFILE[$svc]:-}" ]] && BUILD_SERVICES+=("$svc")
    done
    [[ ${#BUILD_SERVICES[@]} -gt 0 ]] || { echo "No buildable services found" >&2; exit 1; }
fi

echo "=== Building ${#BUILD_SERVICES[@]} images (${BATCH_SIZE} per batch, tag=$TAG) ==="

failures=0
for ((i = 0; i < ${#BUILD_SERVICES[@]}; i += BATCH_SIZE)); do
    batch=("${BUILD_SERVICES[@]:i:BATCH_SIZE}")
    batch_no=$((i / BATCH_SIZE + 1))
    total_batches=$(( (${#BUILD_SERVICES[@]} + BATCH_SIZE - 1) / BATCH_SIZE ))
    echo
    echo "=== Batch $batch_no/$total_batches: ${batch[*]} ==="

    pids=()
    logs=()
    for svc in "${batch[@]}"; do
        dockerfile="${SERVICE_DOCKERFILE[$svc]:-}"
        [[ -n "$dockerfile" && -f "$ROOT_DIR/$dockerfile" ]] || {
            echo "Missing Dockerfile for $svc: $dockerfile" >&2
            failures=$((failures + 1))
            continue
        }

        image="$IMAGE_PREFIX/$svc:$TAG"
        flags=()
        [[ "$PUSH" == "true" ]] && flags+=(--push)
        [[ "$NO_CACHE" == "true" ]] && flags+=(--no-cache)
        [[ "$PULL" == "true" ]] && flags+=(--pull)

        echo "  -> building $image"
        [[ "$DRY_RUN" == "true" ]] && continue

        log_file="$(mktemp "/tmp/build-$svc.XXXXXX.log")"
        logs+=("$log_file")
        docker buildx build "${flags[@]}" -t "$image" -f "$ROOT_DIR/$dockerfile" "$ROOT_DIR" >"$log_file" 2>&1 &
        pids+=("$!")
    done

    [[ "$DRY_RUN" == "true" ]] && continue

    batch_failed=false
    for idx in "${!pids[@]}"; do
        if ! wait "${pids[$idx]}"; then
            echo "  ✗ FAILED build: ${batch[$idx]} (log: ${logs[$idx]})" >&2
            tail -40 "${logs[$idx]}" >&2 || true
            batch_failed=true
            failures=$((failures + 1))
        else
            echo "  ✓ OK: ${batch[$idx]}"
            rm -f "${logs[$idx]}"
        fi
    done

    if [[ "$batch_failed" == "true" ]]; then
        echo "Batch $batch_no failed; aborting remaining batches." >&2
        break
    fi
done

if [[ "$failures" -gt 0 ]]; then
    echo "Build finished with $failures failure(s)." >&2
    exit 1
fi
echo "All images built successfully (tag=$TAG)."
