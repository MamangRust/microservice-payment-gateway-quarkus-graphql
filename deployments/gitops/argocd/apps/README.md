# ArgoCD App-of-Apps — Child Applications

The root Application [`payment-gateway-root`](../root-app.yaml) manages every
child `Application` in this directory. Each child maps to a Kustomize base (or a
per-module production overlay) under `deployments/kubernetes/`.

## Sync Waves

Sync waves (`argocd.argoproj.io/sync-wave`) sequence the rollout so database
migrations complete before domain services start:

| Wave | Applications |
| ---- | ------------ |
| 1 | `common`, `infra-postgres`, `infra-redis`, `infra-kafka` |
| 2 | `pgbouncer`, `service-auth`, `service-user`, `service-role`, `service-card`, `service-merchant`, `service-saldo`, `service-email-service` |
| 3 | `service-topup`, `service-transfer`, `service-withdraw`, `service-transaction` |
| 5 | `service-gateway`, `nginx` |
| 6 | `service-observability` |

## Paths

- **Infra / shared** (`common`, `infra-*`, `pgbouncer`, `nginx`, `service-observability`):
  point directly at `deployments/kubernetes/base/<dir>`.
- **Domain services** (`service-*`): point at per-module overlays
  `deployments/kubernetes/overlays/production/<module>/` which pin the image tag,
  substitute the GHCR owner, and attach the `ghcr-pull-secret` image pull secret.
