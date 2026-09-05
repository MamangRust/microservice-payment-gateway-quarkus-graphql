#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
OUT="$(mktemp)"
trap 'rm -f "$OUT"' EXIT

kubectl kustomize "$ROOT_DIR/deployments/kubernetes/base" > "$OUT"
grep -q 'namespace: payment-gateway' "$OUT"

# Quarkus services expose HTTP on 8084-8093 and gRPC on 9004-9013.
python3 - "$OUT" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text()
docs = text.split("\n---\n")
expected = {
    "auth": 9012,
    "role": 9006,
    "card": 9004,
    "merchant": 9005,
    "user": 9011,
    "saldo": 9007,
    "topup": 9008,
    "transaction": 9009,
    "transfer": 9010,
    "withdraw": 9013,
}
for name, port in expected.items():
    matches = [doc for doc in docs if "kind: Deployment" in doc and f"  name: {name}\n" in doc]
    if len(matches) != 1:
        raise SystemExit(f"expected exactly one Deployment for {name}, found {len(matches)}")
    doc = matches[0]
    for probe in ("livenessProbe:", "readinessProbe:", "startupProbe:"):
        if probe not in doc:
            raise SystemExit(f"{name} missing {probe}")
    if f"containerPort: {port}" not in doc:
        raise SystemExit(f"{name} missing gRPC port {port}")

gateway = [doc for doc in docs if "kind: Deployment" in doc and "  name: gateway\n" in doc]
if len(gateway) != 1 or "containerPort: 8080" not in gateway[0]:
    raise SystemExit("gateway HTTP port 8080 is missing")

jobs = [doc for doc in docs if "kind: Job" in doc]
if not any("  name: migrate\n" in doc and "restartPolicy: OnFailure" in doc for doc in jobs):
    raise SystemExit("migration Job or restartPolicy OnFailure is missing")
PY

echo "Kubernetes manifest contract passed: namespace, Quarkus ports/probes, and migration Job are valid."
echo "Live rollout/rollback remains opt-in: kubectl rollout status/undo require a target cluster."
