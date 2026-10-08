# Kubernetes Resources

Manifest untuk namespace `payment-gateway`. Dikelompokkan **per komponen**, satu
folder per bounded context / service, dan di-apply lewat **kustomize**.

Dulu semua 89 file duduk rata di satu folder. Sekarang struktur-nya:

```
deployments/kubernetes/
├── kustomization.yaml          # root entrypoint (kubectl apply -k deployments/kubernetes)
├── core/                       # Namespace + konfigurasi bersama + ServiceAccount + ExternalSecret
├── security/                   # baseline NetworkPolicy level namespace
├── database/                   # 3 PostgreSQL + 3 PgBouncer + ClickHouse
├── messaging/                  # Kafka (KRaft)
├── cache/                      # Redis
├── observability/              # OTel, Jaeger, Prometheus, Grafana, Loki, exporter
├── networking/                 # Nginx edge
├── services/                   # 15 workload aplikasi, satu subfolder per service
├── components/image-pull-secret/ # patch imagePullSecrets bersama (root + overlay)
└── overlays/production/        # entrypoint ArgoCD: images pinning, GHCR_OWNER, sync-wave
```

`deployments/gitops/argocd/` berisi app-of-apps ArgoCD yang menunjuk ke
`overlays/production` (lihat [Overlay production](#overlay-production)).

Setiap folder punya `kustomization.yaml` sendiri — kustomize menolak direktori
tanpa file itu. Transformer `namespace: payment-gateway` dipakai (sama seperti
repo saudara) tapi praktis no-op karena tiap manifest sudah menulis namespace-nya
sendiri. Yang tetap sengaja **tidak** dipakai adalah `commonLabels`/`namePrefix` —
keduanya bisa mengubah Service selector sampai kehilangan endpoint.

`namespace.yaml` ada di `core/`, bukan di root. Kustomize melarang overlay
memuat file biasa di luar direktori-nya sendiri, jadi Namespace harus ikut salah
satu folder kategori supaya `overlays/production/` bisa mewarisinya. Alasan yang
sama membuat patch `imagePullSecrets` duduk di `components/image-pull-secret/`
yang dirujuk root dan overlay — lihat [Core](#core).

---

## Table of Contents

- [Layout](#layout)
- [Core](#core)
- [Security](#security)
- [Database](#database)
- [Messaging](#messaging)
- [Cache](#cache)
- [Observability](#observability)
- [Networking](#networking)
- [Services](#services)
- [Overlay production](#overlay-production)
- [GitOps (ArgoCD)](#gitops-argocd)
- [How to Use](#how-to-use)
- [Gap yang sudah diperbaiki (putaran pertama)](#gap-yang-sudah-diperbaiki-putaran-pertama)
- [Gap yang sudah diperbaiki (putaran kedua)](#gap-yang-sudah-diperbaiki-putaran-kedua)
- [Gap yang sudah diperbaiki (putaran ketiga)](#gap-yang-sudah-diperbaiki-putaran-ketiga)
- [Sisa catatan](#sisa-catatan)

---

## Layout

| Folder | Isi | Jumlah manifest |
| :--- | :--- | ---: |
| `.` (root) | `kustomization.yaml` saja | 0 |
| `core/` | `namespace.yaml`, ConfigMap `app-config`, ServiceAccount `app`, ExternalSecret `app-secrets`, ExternalSecret `ghcr-pull-secret` | 5 |
| `security/` | 5 NetworkPolicy baseline (default-deny, DNS, intra-namespace, UI eksternal, SMTP) | 5 |
| `database/` | 3 StatefulSet Postgres + 3 PgBouncer + ClickHouse + schema ConfigMap | 8 |
| `messaging/` | Kafka (KRaft), job topic email | 3 |
| `cache/` | Redis StatefulSet + Service + job init cluster | 3 |
| `observability/` | OTel, Jaeger, Prometheus, Grafana, Loki, Alertmanager, exporter | 23 |
| `networking/` | Nginx reverse proxy | 3 |
| `services/` | 15 subfolder aplikasi (masing-masing + `network-policy.yaml`) | 56 |
| `components/image-pull-secret/` | 2 patch JSON (`imagePullSecrets`) + `kustomization.yaml` — **bukan** manifest, tidak dihitung | 0 |
| | **Total** | **106** |

Total resource setelah render: **113** (106 manifest + `namespace.yaml`; beberapa
file berisi lebih dari satu resource).

---

## Core

Konfigurasi yang dipakai bareng. Dimuat ke pod lewat `envFrom`
(`configMapRef: app-config` / `secretRef: app-secrets`).

| File | Kind | Name | Catatan |
| :--- | :--- | :--- | :--- |
| `core/namespace.yaml` | Namespace | `payment-gateway` | dipakai semua resource; di `core/` supaya overlay bisa mewarisinya |
| `core/configmaps.yaml` | ConfigMap | `app-config` | `DB_<CONTEXT>_*`, `GRPC_*_ADDR`, `REDIS_ADDRS`, `KAFKA_BROKERS`, `KAFKA_SERVERS`, `CLICKHOUSE_ADDR`, `OTEL_ENDPOINT`, `METRIC_*_ADDR`, `GHCR_OWNER` |
| `core/serviceaccounts.yaml` | ServiceAccount | `app` | dipakai semua workload, `automountServiceAccountToken: false`, tanpa Role/RoleBinding |
| `core/external-secrets.yaml` | ExternalSecret | `app-secrets` | 7 key dari `ClusterSecretStore payment-gateway-secrets` |
| `core/secrets.yaml` | ExternalSecret | `ghcr-pull-secret` | `.dockerconfigjson` dari `payment-gateway/GHCR_DOCKERCONFIGJSON`; dipakai lewat `imagePullSecrets` |

`DB_HOST` / `DB_PORT` / `DB_NAME` base **sengaja tidak ada** — hanya key
per-context. Kalau satu key lupa diisi, pod gagal start daripada diam-diam
nyambung ke database bounded context lain (lihat `pkg/database/names.go`).

### Secret tidak lagi ada di git

`app-secrets` dan `alertmanager-config` **tidak lagi berupa `Secret` berisi
plaintext**. Keduanya sekarang `ExternalSecret` (External Secrets Operator) yang
menarik nilainya dari backend secret di luar repo.

| ExternalSecret | Key yang dihasilkan | Dipakai oleh |
| :--- | :--- | :--- |
| `app-secrets` | `DB_PASSWORD`, `SECRET_KEY`, `REDIS_PASSWORD`, `GF_SECURITY_ADMIN_PASSWORD`, `CLICKHOUSE_PASSWORD`, `SMTP_USER`, `SMTP_PASS` | 3 Postgres + 3 PgBouncer, Grafana, ClickHouse, 15 pod aplikasi |
| `ghcr-pull-secret` | `.dockerconfigjson` | `imagePullSecrets` di 15 Deployment aplikasi (lewat `components/image-pull-secret/`) |
| `alertmanager-config` | `alertmanager.yml` | Alertmanager (isi file memuat kredensial SMTP) |

Nama key di backend: `payment-gateway/<KEY>` (mis.
`payment-gateway/DB_PASSWORD`), plus `payment-gateway/ALERTMANAGER_CONFIG` untuk
isi `alertmanager.yml` utuh.

**Prasyarat** (lihat [How to Use](#how-to-use)): CRD `external-secrets.io/v1beta1`
dan `ClusterSecretStore` bernama `payment-gateway-secrets` harus sudah ada,
berisi 8 key — 7 untuk `app-secrets` plus `GHCR_DOCKERCONFIGJSON` untuk
`ghcr-pull-secret`. ClusterSecretStore sengaja **tidak** di-commit karena
cluster-scoped dan berisi kredensial ke secret backend — sama seperti repo
saudara (`pos-secrets`, `ecommerce-secrets`).

---

## Security

| File | Kind | Name | Catatan |
| :--- | :--- | :--- | :--- |
| `security/default-deny.yaml` | NetworkPolicy | `default-deny-all` | `podSelector: {}`, tolak semua ingress + egress |
| `security/dns-egress.yaml` | NetworkPolicy | `allow-dns-egress` | egress ke CoreDNS (`kube-system`, `k8s-app: kube-dns`) UDP/TCP 53 |
| `security/allow-intra-namespace.yaml` | NetworkPolicy | `allow-intra-namespace` | ingress dari + egress ke namespace `payment-gateway` |
| `security/external-ingress.yaml` | NetworkPolicy | `allow-external-ui-ingress` | `0.0.0.0/0` → nginx `:80`, grafana `:3000`, jaeger `:16686`/`:14250`, prometheus `:9090` |
| `security/email-smtp-egress.yaml` | NetworkPolicy | `allow-email-smtp-egress` | egress TCP `:587` untuk pod `email` + `alertmanager` |

Di samping itu tiap service punya `services/<svc>/network-policy.yaml`
(`<app>-netpol`, ingress dari + egress ke namespace sendiri) — pola yang sama
dengan repo saudara. Karena `allow-intra-namespace` sudah membuka seluruh
lalu lintas internal, policy per service saat ini **mirror** dari policy itu dan
fungsinya adalah sebagai titik pengetatan per service (mis. `auth` nanti hanya
boleh diakses `apigateway`), bukan penambahan pembatasan.

Hasil akhirnya: **semua** ingress dari luar cluster ditolak kecuali empat UI di
atas, dan **semua** egress ke luar cluster ditolak kecuali DNS dan SMTP.

### RBAC

Semua pod memakai ServiceAccount `app` dengan `automountServiceAccountToken:
false`, dan tidak ada Role/RoleBinding sama sekali. Tidak ada workload di sini
yang memanggil API server, jadi izin paling sedikit = nol izin, dan token API
tidak pernah ada di dalam container.

---

## Database

Satu PostgreSQL per bounded context, masing-masing di depan PgBouncer-nya
sendiri. Service tidak pernah bicara langsung ke PostgreSQL — mereka connect ke
pooler lewat `DB_<CONTEXT>_HOST` / `_PORT` / `_NAME`.

| Context | Prefix env | StatefulSet + headless Service | PgBouncer | Database | Service pemilik |
| :--- | :--- | :--- | :--- | :--- | :--- |
| identity | `DB_IDENTITY` | `postgres-identity.yaml` | `pgbouncer-identity.yaml` | `pg_identity` | auth, role, user |
| payment | `DB_PAYMENT` | `postgres-payment.yaml` | `pgbouncer-payment.yaml` | `pg_payment` | card, merchant, saldo |
| financial | `DB_FINANCIAL` | `postgres-financial.yaml` | `pgbouncer-financial.yaml` | `pg_financial` | topup, transaction, transfer, withdraw |

Tiap file berisi **2 resource**: StatefulSet (atau Deployment untuk PgBouncer) +
Service. Catatan:

- StatefulSet pakai `volumeClaimTemplate` `postgres-data` 20Gi — tidak perlu
  manifest PVC terpisah.
- Service Postgres-nya headless (`clusterIP: None`); PgBouncer yang jadi
  entrypoint (`port: 5432`).
- Password diambil dari `secretKeyRef: app-secrets/DB_PASSWORD`.
- Migrasi goose dijalankan **saat startup service**, bukan lewat Job — dan
  sekarang prefix-aware, jadi tiap service memigrasi database context-nya sendiri.

### ClickHouse (OLAP)

| File | Kind | Name | Catatan |
| :--- | :--- | :--- | :--- |
| `database/clickhouse.yaml` | StatefulSet + Service | `clickhouse` | `clickhouse/clickhouse-server:24.8-alpine`, port 8123 (HTTP) + 9000 (native), VCT `clickhouse-data` 10Gi |
| `database/clickhouse-schema-configmap.yaml` | ConfigMap | `clickhouse-schema` | cerminan `pkg/clickhouse/schema.sql` (7 tabel MergeTree), di-mount ke `/docker-entrypoint-initdb.d` |

Skema di-apply dua jalur:

1. **Initdb mount** — image ClickHouse menjalankan ConfigMap di atas saat data
   dir masih kosong (volume baru). Ini cuma bootstrap.
2. **`clickhouse.ApplySchema()` saat startup** — `stats-reader` dan `stats-writer`
   memanggil `EnsureDatabase` → `NewClient` → `ApplySchema` sebelum mulai
   bekerja. DDL-nya di-`go:embed` langsung dari `pkg/clickhouse/schema.sql`, jadi
   tidak mungkin menyimpang dari file di ConfigMap, dan semua statement
   `CREATE TABLE IF NOT EXISTS` sehingga idempotent.

Jalur kedua inilah yang menutup kasus "volume sudah berisi tapi tabelnya
hilang" — kondisi di mana initdb tidak jalan lagi.

Kredensial: `CLICKHOUSE_USER` = `dragon`, password dari
`app-secrets/CLICKHOUSE_PASSWORD`. Konsumen: `stats-reader` (baca) dan
`stats-writer` (tulis); keduanya mengambil `CLICKHOUSE_ADDR` /
`CLICKHOUSE_DATABASE` / `CLICKHOUSE_USERNAME` dari `app-config` dan password
dari `app-secrets`.

---

## Messaging

| File | Kind | Name | Catatan |
| :--- | :--- | :--- | :--- |
| `messaging/kafka-deployment.yaml` | StatefulSet | `kafka` | 3 replica, KRaft (`KAFKA_PROCESS_ROLES: broker,controller`), VCT `kafka-data` di-mount ke `/var/lib/kafka/data` |
| `messaging/kafka-service.yaml` | Service | `kafka` | `:9092`, `:9093`; sekaligus governing service StatefulSet |
| `messaging/kafka-create-topic-email-job.yaml` | Job | `kafka-create-email-topics` | bikin 13 topic `email-service-topic-*` |

Image-nya **`apache/kafka:4.3.1`** — sama dengan `deployments/local/docker-compose.yml`,
sehingga kontrak env-nya identik di lokal dan di cluster. Dua hal yang berbeda
dari image bitnami sebelumnya:

- Env-nya `KAFKA_*`, bukan `KAFKA_CFG_*`, dan cluster id dibaca dari
  **`CLUSTER_ID`** (bitnami memakai `KAFKA_KRAFT_CLUSTER_ID`; compose sempat
  menulis `KAFKA_CLUSTER_ID` yang diabaikan image).
- `KAFKA_NODE_ID` wajib numerik dan image ini tidak punya trik
  `NODE_ID_COMMAND` ala bitnami. Ordinal pod diturunkan dari `$HOSTNAME`
  (`kafka-0` → `0`) di wrapper `bash -c` yang meng-`exec /etc/kafka/docker/run`;
  `KAFKA_ADVERTISED_LISTENERS` dirakit di wrapper yang sama karena `$(HOSTNAME)`
  tidak bisa di-expand oleh substitusi env Kubernetes.

Zookeeper sudah dihapus — Kafka jalan mode KRaft dan tidak ada satu pun manifest
yang menunjuk zookeeper.

## Cache

| File | Kind | Name | Catatan |
| :--- | :--- | :--- | :--- |
| `cache/redis-deployment.yaml` | StatefulSet | `redis` | 6 replica (cluster mode), VCT `redis-data` |
| `cache/redis-service.yaml` | Service | `redis` | `:6379` |
| `cache/redis-cluster-init-job.yaml` | Job | `redis-cluster-init` | `redis-cli --cluster create` 6 node |

---

## Observability

| File | Kind | Name |
| :--- | :--- | :--- |
| `otel-collector-configmap.yaml` | ConfigMap | `otel-config` |
| `otel-collector-deployment.yaml` | Deployment | `otel-collector` |
| `otel-collector-service.yaml` | Service | `otel-collector` (`:4317`, `:4318`, `:13133`, `:8889`) |
| `jaeger-deployment.yaml` | Deployment | `jaeger` |
| `jaeger-service.yaml` | Service | `jaeger` (LoadBalancer `:16686`, `:14250`) |
| `prometheus-configmap.yaml` | ConfigMap | `prometheus-config` |
| `prometheus-rules-configmap.yaml` | ConfigMap | `prometheus-rules` |
| `prometheus-deployment.yaml` | Deployment | `prometheus` |
| `prometheus-service.yaml` | Service | `prometheus` (LoadBalancer `:9090`) |
| `grafana-deployment.yaml` | Deployment | `grafana` |
| `grafana-service.yaml` | Service | `grafana` (LoadBalancer `:3000`) |
| `loki-configmaps.yaml` | ConfigMap | `loki-config` |
| `loki-deployment.yaml` | Deployment | `loki` |
| `loki-service.yaml` | Service | `loki` (`:3100`) |
| `loki-pv.yaml` | PersistentVolume | `loki-data-pv` |
| `loki-pvc.yaml` | PersistentVolumeClaim | `loki-data-pvc` |
| `alertmanager-config.yaml` | ExternalSecret | `alertmanager-config` |
| `alertmanager-deployment.yaml` | Deployment | `alertmanager` |
| `alertmanager-service.yaml` | Service | `alertmanager` (`:9093`) |
| `node-exporter-daemon.yaml` | DaemonSet | `node-exporter` |
| `node-exporter-service.yaml` | Service | `node-exporter` (`:9100`) |
| `kafka-exporter-deployment.yaml` | Deployment | `kafka-exporter` |
| `kafka-exporter-service.yaml` | Service | `kafka-exporter` (`:9308`) |

---

## Networking

| File | Kind | Name |
| :--- | :--- | :--- |
| `nginx-configmap.yaml` | ConfigMap | `nginx-config` (`upstream apigateway { server apigateway:5000; }`) |
| `nginx-deployment.yaml` | Deployment | `nginx` |
| `nginx-service.yaml` | Service | `nginx` (LoadBalancer `:80`) |

---

## Services

15 workload aplikasi. Tiap folder berisi `deployment` + `hpa` + `service` +
`network-policy`, kecuali yang ditandai.

| Service | Port | DB context | Catatan |
| :--- | :--- | :--- | :--- |
| `apigateway` | 5000, 8091 | — | entry point REST/gRPC |
| `auth` | 50051, 8081 | identity | |
| `role` | 50052, 8082 | identity | |
| `user` | 50055, 8085 | identity | |
| `card` | 50053, 8083 | payment | |
| `merchant` | 50054, 8084 | payment | |
| `saldo` | 50056, 8086 | payment | |
| `topup` | 50057, 8087 | financial | |
| `transaction` | 50058, 8088 | financial | |
| `transfer` | 50059, 8089 | financial | |
| `withdraw` | 50060, 8090 | financial | |
| `email` | 8080 | — | Kafka consumer + SMTP |
| `ai-security` | 50051 | — | tanpa HPA |
| `stats-reader` | 50062 | — | ClickHouse; tanpa HPA |
| `stats-writer` | — | — | Kafka consumer → ClickHouse; **sengaja tanpa Service/HPA** — `cmd/main.go`-nya tidak membuka listener apa pun, jadi tidak ada yang bisa di-connect |

Deployment yang punya DB context menyisipkan initContainer
`wait-for-pgbouncer-<ctx>` (busybox `nc -z pgbouncer-<ctx>… 5432`) supaya tidak
crash-loop sebelum pooler siap. Hampir semua service juga menunggu `kafka` dan
`redis`, dan `stats-reader` / `stats-writer` menunggu `clickhouse`.

---

## Overlay production

`overlays/production/` adalah entrypoint untuk ArgoCD dan untuk `just kube-up`.
Isinya:

| Bagian | Fungsi |
| :--- | :--- |
| `namespace:` | transformer `payment-gateway` (no-op; manifest sudah menulis sendiri) |
| `resources:` | 8 folder kategori didaftarkan eksplisit |
| `images:` | 15 image aplikasi (tempat mem-pin tag) |
| `replacements:` | mengganti segmen owner `__GHCR_OWNER__` dari `app-config/GHCR_OWNER` |
| `components:` | `components/image-pull-secret` (patch `imagePullSecrets`), sama seperti root |
| `patches:` | `postgres-wave.yaml`, `pgbouncer-wave.yaml`, `workload-wave.yaml` |

**Kenapa folder kategori didaftarkan satu per satu, bukan `resources: [../..]`.**
Kustomize menolak overlay yang menunjuk ancestor-nya sendiri (`cycle detected`),
dan load restrictor-nya melarang overlay memuat file biasa — patch,
`namespace.yaml` — di luar direktori overlay itu sendiri. Karena itu
`namespace.yaml` duduk di `core/`, bukan di root.

**Kenapa image ditulis dengan placeholder.** Manifest di folder kategori menulis
`ghcr.io/__GHCR_OWNER__/microservice-payment-gateway-grpc/<svc>:latest` supaya
owner-nya bisa diarahkan per-fork tanpa menyentuh 15 file. Nilai nyatanya
(`mamangrust`) ada di `app-config/GHCR_OWNER`, dan overlay-lah yang
menyubstitusinya. Konsekuensinya: **root `deployments/kubernetes` tidak boleh
dipakai untuk workload aplikasi** — render-nya masih berisi placeholder dan pod
akan `ImagePullBackOff`. Untuk pekerjaan lokal pakai `just kube-up` (yang
menunjuk ke overlay); setelah substitusi, nama image-nya persis sama dengan
`IMAGE_REPO` di justfile, jadi image hasil `just build-image` + `just image-load`
tetap terpakai.

**`newTag` masih `latest`.** CI (`.github/workflows/build_and_push.yaml`) baru
mem-publish tag `latest` + tag branch/semver, belum ada tag commit SHA. Blok
`images:` tetap ditulis supaya saat CI mulai mem-pin SHA immutable, tempat yang
perlu diubah sudah ada — sama seperti repo saudara.

### Urutan sync-wave

| Wave | Resource | Alasan |
| :---: | :--- | :--- |
| `-2` | 3 StatefulSet `postgres-*` | dependency PgBouncer dan semua service |
| `-1` | 3 Deployment `pgbouncer-*` | menunggu PostgreSQL, ditunggu seluruh service |
| — | Kafka, Redis, ClickHouse, observability | tidak diberi wave; service menunggunya lewat initContainer |
| `2` | 15 Deployment aplikasi | menunggu PgBouncer |

Tidak ada wave `0`: pgw menjalankan migrasi goose **saat startup service**, bukan
lewat Job `migrate` seperti repo saudara, jadi tidak ada job yang perlu didahulukan.

---

## GitOps (ArgoCD)

```
deployments/gitops/argocd/
├── project.yaml                     # AppProject payment-gateway
├── root-app.yaml                    # app-of-apps -> deployments/gitops/argocd/production
├── apps/README.md                   # kenapa tidak ada Application per service
└── production/
    ├── kustomization.yaml
    └── production.yaml              # Application -> deployments/kubernetes/overlays/production
```

Satu Application saja (`payment-gateway-production`) yang me-render seluruh
overlay — menambah service tidak perlu Application baru. Bootstrap:

```sh
kubectl apply -f deployments/gitops/argocd/project.yaml
kubectl apply -f deployments/gitops/argocd/root-app.yaml
```

Syaratnya sama seperti repo saudara: CRD `external-secrets.io/v1beta1` dan
`ClusterSecretStore payment-gateway-secrets` harus sudah ada, dan
`app-config/GHCR_OWNER` harus sama dengan `github.repository_owner` (lowercase)
repo yang CI-nya push ke GHCR.

---

## How to Use

### Prasyarat

- [Minikube](https://minikube.sigs.k8s.io/) (driver `docker`)
- [Kubectl](https://kubernetes.io/docs/tasks/tools/) — `kubectl apply -k` butuh ≥ 1.14
- Docker
- **External Secrets Operator + `ClusterSecretStore payment-gateway-secrets`** —
  wajib, karena `app-secrets` / `alertmanager-config` sekarang `ExternalSecret`,
  bukan `Secret`. Tanpa ini `kubectl apply -k` gagal dengan
  `no matches for kind "ExternalSecret"`.

```sh
# 1. CRD + controller ESO
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace --set installCRDs=true

# 2. Pastikan CRD-nya sudah terdaftar
kubectl api-resources | grep -E 'externalsecrets|clustersecretstores'

# 3. ClusterSecretStore `payment-gateway-secrets` (cluster-scoped, di luar repo)
#    berisi kredensial ke secret backend, dengan key:
#      payment-gateway/DB_PASSWORD
#      payment-gateway/SECRET_KEY
#      payment-gateway/REDIS_PASSWORD
#      payment-gateway/GF_SECURITY_ADMIN_PASSWORD
#      payment-gateway/CLICKHOUSE_PASSWORD
#      payment-gateway/SMTP_USER
#      payment-gateway/SMTP_PASS
#      payment-gateway/ALERTMANAGER_CONFIG   (isi alertmanager.yml utuh)
#      payment-gateway/GHCR_DOCKERCONFIGJSON (docker config JSON, read:packages)
kubectl get clustersecretstore payment-gateway-secrets   # harus Ready
```

Untuk cluster lokal yang tidak punya ESO, sebagai gantinya bikin Secret-nya
manual (nilai tidak pernah masuk git):

```sh
kubectl -n payment-gateway create secret generic app-secrets \
  --from-literal=DB_PASSWORD=... --from-literal=SECRET_KEY=... \
  --from-literal=REDIS_PASSWORD=... --from-literal=GF_SECURITY_ADMIN_PASSWORD=... \
  --from-literal=CLICKHOUSE_PASSWORD=... --from-literal=SMTP_USER=... \
  --from-literal=SMTP_PASS=...

# Wajib untuk `just kube-up`: 15 Deployment aplikasi memasang
# imagePullSecrets: ghcr-pull-secret. Kalau secret ini tidak ada, pod gagal
# start ("Unable to retrieve some image pull secrets") — bukan cuma gagal pull.
kubectl -n payment-gateway create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io --docker-username=<user> --docker-password=<PAT>
```

### Quick Start

```sh
minikube start --driver=docker

# Build image tiap service lalu muat ke node minikube
just tidy-all
just build-image
just image-load

# Apply seluruh manifest (kustomize).
# just kube-up menunjuk ke overlays/production, bukan root — lihat
# [Overlay production](#overlay-production).
just kube-up
# atau langsung:
kubectl apply -k deployments/kubernetes/overlays/production

# Verifikasi
just kube-status
minikube tunnel   # supaya Service LoadBalancer dapat IP

# Bersihkan
just kube-down
```

> NetworkPolicy hanya ditegakkan kalau CNI-nya mendukung. Minikube dengan CNI
> default (`bridge`) **tidak** menegakkannya — pakai
> `minikube start --cni=calico` kalau mau policy-nya benar-benar berlaku.

`namespace.yaml` ada di `core/`, bukan di root, dan tetap ikut satu kali apply
lewat `core/kustomization.yaml`. Kubectl menyortir Namespace paling dulu,
sehingga tidak perlu `kubectl apply -f namespace.yaml` terpisah seperti dulu.

### Apply sebagian saja

Karena tiap folder self-contained, folder mana pun bisa di-apply sendiri:

```sh
kubectl apply -k deployments/kubernetes/database
kubectl apply -k deployments/kubernetes/services/auth
kubectl apply -k deployments/kubernetes/observability
```

Catatan: folder kategori di-render apa adanya, jadi image aplikasi masih berisi
placeholder `__GHCR_OWNER__` kalau di-apply tanpa overlay. Ini hanya berguna
untuk infrastruktur (database, cache, messaging, observability); untuk workload
aplikasi pakai `overlays/production`.

### Render tanpa cluster

```sh
kubectl kustomize deployments/kubernetes/overlays/production  # jalur ArgoCD
kubectl kustomize deployments/kubernetes                      # root (image ber-placeholder)
kubectl apply --dry-run=client -k deployments/kubernetes/overlays/production
```

`kubectl kustomize` murni merender dan tidak butuh cluster maupun CRD, jadi itu
cara tercepat memvalidasi perubahan manifest. `--dry-run=client` masih melakukan
lookup RESTMapper, sehingga `ExternalSecret` harus sudah punya CRD-nya.

### Menambah manifest baru

Kustomize tidak bisa glob. Setelah menaruh file baru, **wajib** menambahkan
namanya ke `kustomization.yaml` di folder terkait, kalau tidak file itu tidak
akan ikut ter-render.

Kalau yang ditambah adalah **service baru**, ada dua langkah tambahan:

1. daftarkan folder-nya di `services/kustomization.yaml`;
2. tambahkan image-nya ke blok `images:` di `overlays/production/kustomization.yaml`
   (dengan nama ber-placeholder `ghcr.io/__GHCR_OWNER__/microservice-payment-gateway-grpc/<svc>`)
   dan masukkan namanya ke regex `replacements` + `workload-wave.yaml` — kalau
   terlewat, tag-nya tidak dipin dan owner-nya tidak disubstitusi.

---

## Gap yang sudah diperbaiki (putaran pertama)

Dikerjakan saat memecah 89 manifest flat jadi per-folder:

| # | Gap | Perbaikan |
| :--- | :--- | :--- |
| 1 | ClickHouse tidak punya manifest, `stats-*` menunjuk host tak resolvable | `database/clickhouse.yaml` (StatefulSet + Service) + `database/clickhouse-schema-configmap.yaml`; `CLICKHOUSE_*` dipindah ke `app-config`/`app-secrets` |
| 2 | `stats-writer` tidak punya Service | Bukan gap — `cmd/main.go`-nya cuma consumer Kafka, tidak membuka listener. Dibiarkan tanpa Service, didokumentasikan di tabel Services |
| 3 | `OTEL_ENDPOINT` tidak pernah dibaca kode | `pkg/server.New` sekarang me-resolve `cfg.OtelEndpoint` dari env `OTEL_ENDPOINT` (fallback `localhost:4317`); hardcode `otel-collector:4317` di 11 `cmd/main.go` dihapus. `email` (tidak memakai `pkg/server`) juga dipindah membaca `OTEL_ENDPOINT`, dan `SetDefault` apigateway disamakan ke `localhost:4317` |
| 4 | `app-logs-pvc` ReadWriteOnce dipakai 14 Deployment | Volume-nya **dihapus seluruhnya** (lihat gap #10) |
| 5 | `ai-security` / `stats-reader` / `stats-writer` tidak `envFrom: app-secrets` | Ketiganya ditambah `secretRef: app-secrets`; `REDIS_PASSWORD`, `SMTP_USER`, `SMTP_PASS` dihapus dari `app-config` (dulu plaintext dobel) |
| 6 | Zookeeper vestigial | 3 manifest zookeeper dihapus; Kafka murni KRaft |
| 7 | Naming Service tidak konsisten | 9 Service infra di-rename `<app>-service` → `<app>`; semua DNS ref (configmap, initContainer, `serviceName` StatefulSet) ikut disesuaikan |
| 8 | Image name CI vs manifest beda | Manifest dan justfile sekarang sama-sama `ghcr.io/mamangrust/microservice-payment-gateway-grpc/<svc>:latest` (`IMAGE_REPO` di justfile). Sejak overlay ditambahkan (putaran ketiga), manifest menulis `__GHCR_OWNER__` dan overlay yang mengisi `mamangrust` — hasil render-nya tetap sama |
| 9 | Kafka image drift | `bitnami/kafka` di-pin ke `3.7.0` di StatefulSet dan Job (lalu digantikan, lihat gap #11) |

Tambahan yang ketemu saat perbaikan:

- **`ai-security` membaca `KAFKA_SERVERS`, bukan `KAFKA_BROKERS`** — key `KAFKA_SERVERS` ditambahkan ke `app-config`, kalau tidak ai-security jatuh ke default `localhost:9092`.
- **`REDIS_PASSWORD` dulu plaintext di `app-config`** padahal juga ada di `app-secrets` — sekarang hanya di Secret.

---

## Gap yang sudah diperbaiki (putaran kedua)

| # | Gap | Perbaikan |
| :--- | :--- | :--- |
| 10 | **`app-logs-pvc` hostPath** dipakai 14 Deployment | Volume-nya dihapus. Ternyata **tidak ada satu pun kode Go yang menulis ke `/var/log/app`** — `pkg/logger` hanya `zapcore.AddSync(os.Stdout)` — dan Promtail tidak di-deploy di k8s maupun compose. Jadi PV/PVC + initContainer `init-log-permission` + `volumeMounts` itu sisa desain lama. Log keluar lewat stdout → container runtime / OTel collector. `storage/` dihapus, 14 Deployment dibersihkan |
| 11 | **Kafka pakai `bitnami/kafka`, compose pakai `apache/kafka`** | StatefulSet + Job topic → **`apache/kafka:4.3.1`**, sama dengan compose (yang juga di-pin, tidak lagi `latest`). Env dikonversi ke kontrak apache (`KAFKA_*`, `CLUSTER_ID`, `KAFKA_LOG_DIRS`), ordinal pod diturunkan dari `$HOSTNAME`, dan VCT `kafka-data` yang tadinya **tidak pernah di-mount** sekarang benar-benar dipakai |
| 12 | **Skema ClickHouse hanya di-apply saat volume kosong** | `pkg/clickhouse/schema.go` baru: `ApplySchema()` dengan DDL di-`go:embed` dari `schema.sql`. Ditambah `EnsureDatabase()` di `connect.go`. Keduanya dipanggil `stats-reader` & `stats-writer` saat startup. ConfigMap initdb tetap ada tapi sekarang cuma bootstrap |
| 13 | **Tidak ada `ServiceAccount`/RBAC** | `core/serviceaccounts.yaml`: ServiceAccount `app`, `automountServiceAccountToken: false`, tanpa Role/RoleBinding. Dipakai seluruh 35 workload |
| 14 | **Tidak ada `NetworkPolicy`** | `security/` berisi 5 policy baseline (default-deny, DNS, intra-namespace, UI eksternal, SMTP) + `network-policy.yaml` per service. Hasilnya: ingress luar hanya ke 4 UI, egress luar hanya DNS + SMTP |
| 15 | **Secret plaintext di `secret.yaml`** | `core/secret.yaml` dihapus. `app-secrets` + `alertmanager-config` sekarang `ExternalSecret` lewat `ClusterSecretStore payment-gateway-secrets`, pola yang sama dengan repo saudara (`pos-secrets`, `ecommerce-secrets`). Nol plaintext di git |

Catatan: gap #11 dan #12 di repo saudara sudah lama beres, jadi sekarang ketiga
repo konsisten di dua titik itu.

---

## Gap yang sudah diperbaiki (putaran ketiga)

Dikerjakan saat menyamakan struktur dengan repo saudara (`microservice-ecommerce-grpc`,
`microservice-pointofsale-grpc`), yang sudah punya `overlays/production` + `deployments/gitops`:

| # | Gap | Perbaikan |
| :--- | :--- | :--- |
| 16 | **Tidak ada overlay production** — manifest dan justfile terikat nama image konkret, tidak ada tempat untuk mem-pin tag per environment | `overlays/production/` dengan `images:` (15 service), `replacements` `GHCR_OWNER`, dan patch sync-wave. Manifest menulis `__GHCR_OWNER__`; `app-config` dapat key `GHCR_OWNER` |
| 17 | **Tidak ada urutan apply** — PgBouncer bisa dibuat sebelum PostgreSQL siap | `postgres-wave.yaml` (`-2`), `pgbouncer-wave.yaml` (`-1`), `workload-wave.yaml` (`2`) |
| 18 | **Tidak ada GitOps** | `deployments/gitops/argocd/` (`project.yaml`, `root-app.yaml`, `production/`) — satu Application app-of-apps ke `overlays/production` |
| 19 | **`namespace.yaml` di root** tidak bisa diwarisi overlay | dipindah ke `core/namespace.yaml` |
| 20 | **Tidak ada `imagePullSecrets`** — pod menarik image dari GHCR tanpa kredensial; paket GHCR default-nya private, jadi jalur ArgoCD bakal `ImagePullBackOff` | `core/secrets.yaml` (ExternalSecret `ghcr-pull-secret`) + `components/image-pull-secret/` yang dipakai root & overlay. Sekaligus menyamakan file-set dengan repo saudara |

Konsekuensi yang perlu diingat: `kubectl apply -k deployments/kubernetes` (root)
**tidak lagi** bisa dipakai untuk workload aplikasi karena image-nya masih
ber-placeholder. `just kube-up` sudah diarahkan ke `overlays/production`.

Konsekuensi kedua: karena 15 Deployment sekarang memasang
`imagePullSecrets: ghcr-pull-secret`, cluster lokal tanpa ESO **wajib** membuat
secret itu manual (`kubectl create secret docker-registry ghcr-pull-secret ...`)
sebelum `just kube-up` — kalau tidak, pod gagal start, bukan sekadar gagal pull.

## Sisa catatan

Bukan gap yang perlu ditutup, tapi batasan yang perlu diketahui:

| # | Hal | Kenapa dibiarkan |
| :--- | :--- | :--- |
| 1 | **`observability/loki-pv.yaml` masih `hostPath`** | Loki jalan 1 replica dengan storage sendiri, jadi tidak kena masalah multi-writer seperti `app-logs` dulu. Tetap single-node: di cluster multi-node perlu storage class |
| 2 | **`node-exporter` pakai `hostPath` `/proc`, `/sys`, `/`** | Memang begitu cara node-exporter bekerja — bukan sesuatu yang bisa dihindari |
| 3 | **Belum ada `Ingress`** | Nginx masih `Service type: LoadBalancer`. Ingress butuh ingress controller + TLS, pekerjaan tersendiri |
| 4 | **NetworkPolicy butuh CNI yang menegakkannya** | Minikube default (`bridge`) mengabaikannya; jalankan `minikube start --cni=calico` kalau mau policy-nya berlaku |
| 5 | **Skema ClickHouse tidak di-version** | `ApplySchema` idempotent, tapi kalau nanti kolom berubah perlu `ALTER TABLE` — itu butuh versioning tersendiri |
| 6 | **`observability/promtail-config.yaml` (di root repo) sudah tidak relevan** | Referensinya ke `/var/log/app/*.log` yang tidak pernah ditulis kode. Dibiarkan, tidak dihapus, supaya perubahan ini fokus ke k8s |

