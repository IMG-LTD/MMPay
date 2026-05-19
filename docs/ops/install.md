# Install

MMPay can run locally as a minimal web and backend stack after the app image is
built. This path is for local validation and MP-8 preparation; Huifu sandbox
credentials, provider private keys, and license signing keys remain outside this
repository.

## Prerequisites

- Java 21 and Maven for local validation.
- Node 22 and pnpm 10 for the admin frontend checks.
- Docker with access to the local Docker socket.

## Validate

```bash
bash scripts/validate-local.sh
```

## v1 Tag Immutability For Mirrors

GitHub canonical releases use the checked-in
`governance/github-rulesets/v1-tags.json` ruleset. Self-hosted mirrors must
also reject tag deletion and non-fast-forward updates:

```bash
git config receive.denyDeletes=true
git config receive.denyNonFastForwards=true
```

Do not mirror `v1.*` tags to a server that allows tag rewrites.

## Build The Local Image

```bash
docker build -t mmpay-app:local .
```

## Start Services

Use the standard local compose profile when you want Docker Compose to build the
app image and keep database/cache data in named volumes:

```bash
docker compose -f deploy/docker-compose.yml up --build
```

Use the minimal profile when the `mmpay-app:local` image already exists and you
want the smallest runtime stack:

```bash
docker compose -f deploy/docker-compose.minimal.yml up
```

The bundled admin UI is served from `http://localhost:8080/`. The backend
exposes Spring Boot actuator health on `http://localhost:8080/actuator/health`.

## Helm Chart

The optional Helm chart is app-only. It deploys `mmpay-app` and expects the
operator to provide PostgreSQL, Redis, and a Kubernetes Secret that contains
provider credential values. The chart never creates Huifu credentials or MMMail
license signing material.

For Huifu sandbox preparation, store these keys in the external Secret instead
of source files: `HUIFU_SYS_ID`, `HUIFU_PRODUCT_ID`, `HUIFU_RSA_PUBLIC_KEY`,
`HUIFU_RSA_PRIVATE_KEY`, `HUIFU_SKILL_SOURCE`, `HUIFU_MERCHANT_ID`,
`HUIFU_NOTIFY_URL`, and `HUIFU_WEBHOOK_ENDPOINT_KEY`.

Validate the chart structure before using it:

```bash
bash scripts/validate-helm-chart.sh
```

Create a Secret with the key names configured in
`deploy/helm/mmpay/values.yaml`, then override the external service endpoints:

```bash
helm upgrade --install mmpay deploy/helm/mmpay \
  --set app.datasource.url=jdbc:postgresql://postgres.example:5432/mmpay \
  --set app.redis.url=redis://redis.example:6379 \
  --set secrets.existingSecret=replace-with-mmpay-secret
```

## Evidence Boundary

This local install path does not prove end-to-end payment completion by itself.
The MP-8 evidence file must be generated from a real sandbox or live run and
validated separately:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
