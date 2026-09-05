# Kubernetes Production Deployment

The manifests under `base/` describe the Quarkus payment-gateway microservices
and their supporting infrastructure. They are intentionally not a complete
cluster bootstrap: the following prerequisites must exist before an ArgoCD sync.

## Layout

```text
deployments/kubernetes/
├── base/                          # environment-agnostic resources
│   ├── kustomization.yaml         # aggregate of all modules (kubectl kustomize)
│   ├── common/                    # Namespace, app-config ConfigMap, ExternalSecrets
│   ├── postgres/  pgbouncer/  redis/  kafka/
│   ├── db-migration/              # Flyway migration Job
│   ├── gateway/  auth/  user/  role/  card/  merchant/  saldo/
│   ├── topup/  transaction/  transfer/  withdraw/  email-service/
│   ├── nginx/                     # reverse proxy → gateway:8080
│   └── observability/             # Prometheus, Grafana, OTel, Jaeger, Loki, exporters
└── overlays/production/           # ArgoCD-deployed layer
    ├── kustomization.yaml         # aggregate overlay (full stack in one app)
    ├── <module>/kustomization.yaml# per-module overlay (App-of-Apps child apps)
    ├── migration-hook.yaml        # PreSync hook annotations for the migrate Job
    ├── image-pull-secret-patch.yaml / job-image-pull-secret-patch.yaml
    ├── email-smtp-egress.yaml     # NetPol: email-service → SMTP :587
    └── dns-egress.yaml            # NetPol: cluster-wide DNS egress
```

Each service module ships its own `kustomization.yaml` (Deployment + Service +
HPA + PodDisruptionBudget + NetworkPolicy), so it can be rendered standalone —
this is what the ArgoCD App-of-Apps child applications point at.

## Required cluster prerequisites

1. **External Secrets Operator** must be installed and serving
   `external-secrets.io/v1beta1`.
2. A `ClusterSecretStore` named `payment-gateway-secrets` must exist and provide
   these remote keys:
   `payment-gateway/DB_USERNAME`, `DB_PASSWORD`, `POSTGRES_USER`,
   `POSTGRES_PASSWORD`, `GF_SECURITY_ADMIN_PASSWORD`, `SECRET_KEY`,
   `REDIS_PASSWORD`, `SMTP_USER`, `SMTP_PASS`, `ALERTMANAGER_CONFIG`, and
   `GHCR_DOCKERCONFIGJSON`. `ALERTMANAGER_CONFIG` must contain the complete
   Alertmanager YAML (including SMTP settings) as one secret value.
3. The `GHCR_DOCKERCONFIGJSON` value must be a valid Docker config JSON with
   `read:packages` access to the GHCR image repo. The image owner is templated:
   it is read from the `app-config` ConfigMap key `GHCR_OWNER`
   (`deployments/kubernetes/base/common/configmaps.yaml`) and substituted into
   the `<owner>` segment of every image reference by the `images`/`replacements`
   blocks in `overlays/production/`. Keep `GHCR_OWNER` equal to
   `github.repository_owner` (lowercase) of the CI repo — on a fork, change only
   that ConfigMap value.
4. The cluster must have metrics-server installed before enabling the HPAs.
5. Apply and verify the External Secrets resources server-side before syncing
   application Deployments:

```bash
kubectl apply --server-side -k deployments/kubernetes/base/common
kubectl -n payment-gateway get externalsecret app-secrets ghcr-pull-secret
kubectl -n payment-gateway get secret app-secrets ghcr-pull-secret
```

## Ports (matching Quarkus application.properties)

| Module | HTTP | gRPC | Module | HTTP | gRPC |
| ------ | ---- | ---- | ------ | ---- | ---- |
| gateway | 8080 | – | topup | 8088 | 9008 |
| auth | 8092 | 9012 | transaction | 8089 | 9009 |
| user | 8091 | 9011 | transfer | 8090 | 9010 |
| role | 8086 | 9006 | withdraw | 8093 | 9013 |
| card | 8084 | 9004 | email-service | 8094 | – |
| merchant | 8085 | 9005 | pgbouncer | 6432 | – |
| saldo | 8087 | 9007 | | | |

## Image promotion

CI publishes each application image to:

```text
ghcr.io/<owner>/monolith-payment-gateway-grpc-quarkus/<service>:<git-sha>
ghcr.io/<owner>/monolith-payment-gateway-grpc-quarkus/<service>:latest
```

where `<owner>` is `github.repository_owner` (lowercase) and must match the
`GHCR_OWNER` value in the `app-config` ConfigMap (see prerequisites above).

The base manifests use `:latest`, and each per-module production overlay pins an
immutable tag per release. To verify manifests before rollout:

```bash
kubectl kustomize deployments/kubernetes/base > /dev/null
kubectl kustomize deployments/kubernetes/overlays/production > /dev/null
kubectl kustomize deployments/kubernetes/overlays/production/auth > /dev/null
```

## ArgoCD

See [deployments/gitops/argocd](../gitops/argocd/apps/README.md) for the
App-of-Apps setup (`payment-gateway-root`, child applications, sync waves 1–6).
