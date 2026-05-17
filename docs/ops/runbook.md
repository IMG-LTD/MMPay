# Runbook

This runbook covers the local minimal profile. It does not replace MP-8 sandbox
evidence collection.

## Start

```bash
docker build -t mmpay-app:local .
docker compose -f deploy/docker-compose.minimal.yml up
```

## Health Check

```bash
curl -fsS http://localhost:8080/actuator/health
```

Expected result: Spring Boot returns an actuator health response. If Docker
socket access fails, fix host permissions first; do not bypass the Docker build
with a fake image.

## Admin Dashboard State

Before real Huifu credentials are connected, `/api/admin/dashboard` reports the
channel status as `credentials-required` and runtime payment tables remain empty.
This is intentional and prevents seeded fake payment success rows.

## Evidence Check

After a real sandbox or live run, validate the redacted evidence file:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
