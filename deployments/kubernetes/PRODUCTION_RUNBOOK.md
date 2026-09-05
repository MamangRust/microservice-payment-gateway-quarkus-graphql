# Production Deployment Runbook

## Scope

Production is deployed only through the ArgoCD **App-of-Apps**:

```text
deployments/gitops/argocd/root-app.yaml                          (payment-gateway-root)
  -> deployments/gitops/argocd/apps/*.yaml                       (child Applications, sync waves 1-6)
       -> deployments/kubernetes/overlays/production/<module>/   (per-module Kustomize overlays)
       -> deployments/kubernetes/base/<module>/                  (bases)
```

Do not enable the legacy per-service ArgoCD Applications at the same time as
`payment-gateway-root`; they target overlapping resources and can fight over
ownership.

## Prerequisites

- Kubernetes context points to the intended production cluster.
- External Secrets Operator is installed.
- `ClusterSecretStore/payment-gateway-secrets` is Ready.
- `Secret/app-secrets` and `Secret/ghcr-pull-secret` can be materialized.
- Metrics Server is installed for HPA.
- GHCR package access is valid for all 13 payment-gateway application images.
- PostgreSQL, Kafka, Redis, and storage classes are healthy.

Verify before sync:

```bash
kubectl config current-context
kubectl -n external-secrets get pods
kubectl get clustersecretstore payment-gateway-secrets
kubectl -n payment-gateway get secret app-secrets ghcr-pull-secret
kubectl api-resources | grep -E 'externalsecrets|clustersecretstores'
```

## Promote a release

Use the exact SHA produced by CI. Never use `latest` in production:

```bash
./deployments/kubernetes/promote-image.sh "$GITHUB_SHA"
./deployments/kubernetes/validate-production.sh
kubectl kustomize deployments/kubernetes/overlays/production >/tmp/payment-gateway-production.yaml
```

Review the rendered diff, then commit the promoted SHA. CI must publish all
13 images for that SHA before ArgoCD is synced.

## Server-side validation and sync

```bash
kubectl apply --server-side --dry-run=server \
  -k deployments/kubernetes/overlays/production
kubectl diff -k deployments/kubernetes/overlays/production
kubectl apply -f deployments/gitops/argocd/root-app.yaml
```

ArgoCD ordering (sync waves):

1. Wave 1 — namespace/infrastructure: `common`, `infra-postgres`, `infra-redis`, `infra-kafka`.
2. Wave 2 — `db-migration` (`migrate` PreSync hook, recreated for the promoted image; `kafka-create-email-topics` Sync hook).
3. Wave 3 — `pgbouncer` + identity/card/merchant/saldo/email services.
4. Wave 4 — financial movement services (`topup`, `transfer`, `withdraw`, `transaction`).
5. Wave 5 — `service-gateway`, `nginx`.
6. Wave 6 — `service-observability`.

The migration hook must succeed before the wave-3 application rollout is
considered healthy. Do not manually delete migration Jobs during a sync.

## Post-deploy verification

```bash
kubectl -n payment-gateway get applications -o wide
kubectl -n payment-gateway get pods -o wide
kubectl -n payment-gateway get events --sort-by=.lastTimestamp | tail -50
kubectl -n payment-gateway get hpa,pdb
kubectl -n payment-gateway rollout status deployment/gateway --timeout=10m
kubectl -n payment-gateway get endpoints gateway
kubectl -n payment-gateway port-forward svc/nginx 8080:80
curl -fsS http://127.0.0.1:8080/q/health
```

Check metrics-server, target health, Kafka consumer lag, PostgreSQL exporter,
OTel/Jaeger traces, and alert rules before declaring the release healthy.

## Rollback

Rollback is a Git revert or a new promotion to the previous immutable SHA:

```bash
./deployments/kubernetes/promote-image.sh "$PREVIOUS_SHA"
git commit -am "rollback production to $PREVIOUS_SHA"
```

Then allow ArgoCD to reconcile and verify:

```bash
kubectl -n payment-gateway rollout status deployment/gateway --timeout=10m
kubectl -n payment-gateway get pods
```

If the migration is not backward-compatible, stop and restore the database
from the approved backup before rolling application code back.

## Backup and restore gate

Before production go-live, test a PostgreSQL logical backup and restore into a
separate database/cluster. Record the backup timestamp, migration version,
restore duration, and verification query. A successful manifest sync alone is
not proof of disaster-recovery readiness.
