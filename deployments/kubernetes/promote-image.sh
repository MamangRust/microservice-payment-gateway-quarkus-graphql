#!/usr/bin/env bash
set -euo pipefail

sha="${1:-${GITHUB_SHA:-}}"
if [[ ! "$sha" =~ ^[0-9a-f]{40}$ ]]; then
  echo "usage: $0 <40-character git SHA>" >&2
  exit 2
fi

overlay="deployments/kubernetes/overlays/production"

# Pin the immutable tag in the aggregate overlay and every per-module overlay
# (gateway, auth, user, role, card, merchant, saldo, topup, transaction,
# transfer, withdraw, email-service).
for file in "$overlay/kustomization.yaml" "$overlay"/*/kustomization.yaml; do
  if grep -q 'newTag:' "$file"; then
    sed -i -E "s/newTag: .*/newTag: ${sha}/g" "$file"
    echo "Pinned $file -> ${sha}"
  fi
done

echo "Done. All production image tags pinned to ${sha}."
