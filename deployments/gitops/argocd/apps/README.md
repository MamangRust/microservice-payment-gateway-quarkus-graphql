# ArgoCD Applications

This directory intentionally contains no ArgoCD Application manifests.

The ArgoCD app-of-apps is a **single application**: `payment-gateway-production`
(defined in `../production/production.yaml`), which points to
`deployments/kubernetes/overlays/production`. That overlay renders the entire
stack (`deployments/kubernetes` category folders) with the production image
overrides (`newTag` in `images:`) and the `GHCR_OWNER` templating.

The overlay is the only entrypoint that produces pullable image references:
the base manifests write `ghcr.io/__GHCR_OWNER__/microservice-payment-gateway-grpc/<svc>`
and the `replacements` block substitutes the real owner from
`app-config/GHCR_OWNER`. Applying the root `deployments/kubernetes` directly
leaves the placeholder in place, so it must not be used for application
workloads.

Adding a new service requires no new ArgoCD Application — drop its folder under
`deployments/kubernetes/services/`, list it in `services/kustomization.yaml`,
and add its image to the overlay's `images:` block; it is picked up
automatically via the root/overlay.
