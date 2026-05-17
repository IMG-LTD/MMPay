# Install

MMPay can run locally as a minimal backend stack after the app image is built.
This path is for local validation and MP-8 preparation; Huifu sandbox
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

## Build The Local Image

```bash
docker build -t mmpay-app:local .
```

## Start Minimal Services

```bash
docker compose -f deploy/docker-compose.minimal.yml up
```

The backend exposes Spring Boot actuator health on port `8080`.

## Evidence Boundary

This local install path does not prove end-to-end payment completion by itself.
The MP-8 evidence file must be generated from a real sandbox or live run and
validated separately:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
