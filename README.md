# MMPay

MMPay is an independent payment gateway project for self-hosted products. Its
first integration target is MMMail, but the gateway is not part of the MMMail
source tree and must remain in its own repository.

## Status

`v0.1.1` is the public MP-8 preparation release for deploying MMPay to a public
test server, opening the bundled admin UI, wiring Huifu sandbox callbacks, and
collecting external evidence for MMMail subscription and license relay flows. It
is not a full payment-closure or GA evidence release.

This repository has the MP-0 through MP-7 scaffold in place:

- Backend foundation: Pig (Spring Cloud Alibaba) compatible Spring Boot app
  baseline, with payment domain modules split under `backend/`.
- Payment domain: payment intent, transaction, refund, reconciliation, and
  Flyway migration contracts.
- Provider adapter: Huifu reconciliation mapping plus signed create, query, and
  refund request preparation. Live provider execution remains unavailable until
  a real endpoint is wired and evidenced.
- Outbound webhook: MMMail-compatible HMAC signature contract.
- License boundary: relay-only delivery; no license signing module exists here.
- Admin surface: read-only dashboard API plus soybean-admin stack frontend using
  Vue 3, Vite, Pinia, and Naive UI. Merchant, channel, order, refund, webhook,
  and reconciliation views are present; runtime tables remain empty until a real
  provider connection exists. Credential fields display only secret handles.
- Deployment: Docker Compose and an app-only Helm chart exist for the runnable
  baseline. The Docker image bundles the Spring Boot API and built
  `frontend-admin` static assets, so `/` serves the admin UI while `/api/*` and
  `/actuator/*` stay on the backend. The Helm chart expects external
  PostgreSQL, Redis, and Kubernetes Secret references; it does not create
  provider credentials.

Disabled Pig modules for this phase: code generation, full auth center,
standalone gateway cluster, distributed job scheduler, and unrelated sample
business modules. MMPay keeps the Pig-style Spring Cloud Alibaba foundation but
only enables the minimal app surface required by the payment gateway.

## Security Boundary

- Merchant credentials, provider private keys, customer secrets, and license
  signing private keys must never be committed.
- MMPay emits payment facts. It does not issue, self-sign, or generate MMMail
  licenses.
- License issuance remains an IMG-LTD vendor-controlled process outside this
  repository.

## Docker Deployment

For a source-based server deployment, clone this repository and run:

```bash
docker compose -f deploy/docker-compose.yml up --build -d
```

For a prebuilt-image deployment after the `MMPay Images` workflow publishes
`v0.1.1`, use:

```text
ghcr.io/img-ltd/mmpay-app:v0.1.1
```

Runtime credentials must be injected through environment variables, secret
files, or the Helm chart's external Kubernetes Secret references. Do not place
Huifu merchant credentials, RSA private keys, webhook secrets, or license
signing material in this repository.

The backend health endpoint is:

```bash
curl -fsS http://localhost:8080/actuator/health
```

The bundled admin UI is served from:

```text
http://localhost:8080/
```

For Huifu sandbox callbacks, configure `HUIFU_NOTIFY_URL` to a public HTTPS URL
that reaches the deployed MMPay callback endpoint. A localhost URL cannot
receive provider callbacks from Huifu.

## Local Validation

```bash
bash scripts/validate-local.sh
```
