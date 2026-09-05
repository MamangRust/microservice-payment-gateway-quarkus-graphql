#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
overlay="$root/deployments/kubernetes/overlays/production"
rendered="$(mktemp)"
trap 'rm -f "$rendered"' EXIT

# Validate the aggregate overlay (full stack).
kubectl kustomize "$overlay" > "$rendered"

grep -q 'kind: ExternalSecret' "$rendered"
grep -q 'argocd.argoproj.io/hook: PreSync' "$rendered"
grep -q 'kind: Namespace' "$rendered"
grep -q 'name: payment-gateway' "$rendered"
# Immutable release tags only — reject :latest on the CI-promoted application
# images (base uses :latest by design; the production overlay must pin a SHA).
# Infra images (pgbouncer, node-exporter, ...) are not pipeline-promoted.
if grep -Eq 'image: .*monolith-payment-gateway-grpc-quarkus/.*:(latest|1\.0)([[:space:]]|$)' "$rendered"; then
  echo "ERROR: production manifests still contain mutable image tags (:latest)" >&2
  exit 1
fi
! grep -Eq 'port: 0([[:space:]]|$)' "$rendered"
! grep -Eq '^[[:space:]]+(DB_PASSWORD|POSTGRES_PASSWORD|SECRET_KEY|REDIS_PASSWORD|SMTP_PASS):[[:space:]]+"[^$]' "$root/deployments/kubernetes/base/common/secrets.yaml"

# Validate every per-module overlay renders standalone (App-of-Apps paths).
modules=(gateway auth user role card merchant saldo topup transaction transfer withdraw email-service)
for m in "${modules[@]}"; do
  kubectl kustomize "$overlay/$m" > /dev/null
  echo "  validated overlay/$m"
done

# Validate the infra base dirs standalone (child apps point at them directly).
base="$root/deployments/kubernetes/base"
infra=(common postgres redis kafka pgbouncer nginx observability)
for m in "${infra[@]}"; do
  kubectl kustomize "$base/$m" > /dev/null
  echo "  validated base/$m"
done

echo "Production manifest validation passed"
