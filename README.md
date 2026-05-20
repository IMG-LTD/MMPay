# MMPay

MMPay is an independent payment gateway for self-hosted products. Its first
supported integration target is MMMail, but MMPay is released, validated, and
deployed from this standalone repository.

## Release Status

`v1.0.0` is the first GA release. The GitHub Release is published at
`https://github.com/IMG-LTD/MMPay/releases/tag/v1.0.0`, and the `MMPay Images`
workflow for tag `v1.0.0` completed successfully on commit
`ac19a23b4b297cf8bf83ccf5ba749ad78cc3aa22`.

The GA scope includes:

- Backend foundation for setup, RBAC, audit chain, IAM boundaries, and degraded
  startup blocking for payment mutation surfaces.
- Payment lifecycle APIs for payment intents, provider callbacks, refunds,
  reconciliation acknowledgement, outbound webhook integrations, delivery logs,
  redispatch, and guarded bulk redispatch.
- Huifu adapter request preparation, reconciliation mapping, callback signature
  verification, and redacted sandbox evidence for the v1.0.0 external run.
- Merchant, channel, credential reference, credential binding, and binding
  verification admin workflows.
- License relay delivery only. MMPay does not issue, generate, or sign MMMail
  licenses.
- Soybean Admin based `frontend-admin` console with MMPay pages for operators.
- Docker Compose, app-only Helm chart, image digest evidence, backup/restore
  drill evidence, vendor binding evidence, and v1 tag ruleset evidence.

Pig backend alignment remains documented in
`docs/architecture/pig-backend-alignment.md`. Public documents should describe
the runtime as Pig-aligned or Pig migration in progress until Pig auth, gateway,
and upms become the active runtime.

## Security Boundary

- Merchant credentials, provider private keys, customer secrets, webhook
  secrets, and license signing private keys must never be committed.
- Runtime credentials must come from environment variables, secret files,
  Kubernetes Secrets, or an external secret manager.
- MMPay emits payment facts and relays license claims as opaque bytes. License
  issuance remains an IMG-LTD vendor-controlled process outside this repository.
- Provider and license failures must remain explicit. The repository must not
  introduce mock paid states, fake provider success paths, or silent fallbacks.

## Deployment

Source-based local deployment:

```bash
export MMPAY_AUDIT_HMAC_KEY="$(openssl rand -base64 32)"
docker compose -f deploy/docker-compose.yml up --build --force-recreate -d
```

Run the compose smoke when validating a fresh local deployment:

```bash
bash scripts/smoke-docker-compose.sh
```

Do not start the app image with bare `docker run` unless you also provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, and `MMPAY_AUDIT_HMAC_KEY`. The Docker image
intentionally fails before Spring Boot starts when those runtime values are
missing.

If logs show `Failed to configure a DataSource`, rebuild through Docker Compose
with `--build --force-recreate`; that message means the app was started without
the Compose-injected JDBC environment or from an old local image.

Published v1.0.0 images:

```text
ghcr.io/img-ltd/mmpay-app:v1.0.0
ghcr.io/img-ltd/mmpay-frontend-admin:v1.0.0
ghcr.io/img-ltd/mmpay-app-debug-symbols:v1.0.0
```

The root Docker image bundles the Spring Boot API and built `frontend-admin`
static assets. `/` serves the admin UI, while `/api/*` and `/actuator/*` remain
backend routes.

Health check:

```bash
curl -fsS http://localhost:8080/actuator/health
```

Admin UI:

```text
http://localhost:8080/
```

For Huifu sandbox callbacks, configure `HUIFU_NOTIFY_URL` to a public HTTPS URL
that reaches the deployed MMPay callback endpoint. Localhost callback URLs
cannot receive provider callbacks from Huifu.

## Validation And Evidence

Local validation:

```bash
bash scripts/validate-local.sh
```

GA release gate:

```bash
bash scripts/release-gate.sh --ga
```

Release evidence:

- `docs/release/v1.0.0-release-notes.md`
- `docs/release/v1.0.0-image-digest-evidence.md`
- `docs/release/v1.0.0-e2e-evidence.md`
- `docs/release/backup-restore-drill-evidence.md`
- `docs/release/vendor-binding/v1.0.0-BINDING_OK.asc`
- `docs/release/v1.0.0-v1-tag-ruleset-evidence.md`
