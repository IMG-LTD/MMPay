# Release Process

MMPay release automation has a local gate, a GitHub Release workflow, a GHCR
image publishing workflow, and dependency surface governance. Do not publish a
release from an unvalidated working tree.

## Local release candidate gate

Run the full local gate before creating a tag or dispatching image publishing:

```bash
bash scripts/validate-local.sh
```

This gate covers secret scanning, migration naming, backend compilation, the
admin dashboard API contract, repository governance contracts, and the
soybean-admin frontend checks. It also runs `scripts/validate-helm-chart.sh` to
confirm the app-only Helm chart uses the MMPay image, external secret
references, and actuator probes without embedding provider credentials.

## GitHub Release workflow

The `MMPay Release` workflow runs `bash scripts/release-gate.sh` before it
creates a GitHub Release. It only publishes an existing `v*` tag and uses
`gh release create --verify-tag`, so a missing or mistyped tag fails explicitly.
Release notes are loaded from `docs/release/<tag>-release-notes.md`; a missing
notes file is a hard workflow failure.

## Image publishing

The `MMPay Images` workflow builds the root `Dockerfile` and publishes:

```text
ghcr.io/img-ltd/mmpay-app:<git-ref>
ghcr.io/img-ltd/mmpay-app:<commit-sha>
ghcr.io/img-ltd/mmpay-frontend-admin:<git-ref>
ghcr.io/img-ltd/mmpay-frontend-admin:<commit-sha>
ghcr.io/img-ltd/mmpay-app-debug-symbols:<git-ref>
ghcr.io/img-ltd/mmpay-app-debug-symbols:<commit-sha>
```

Tags are only evidence after the remote workflow has completed successfully and
the immutable image digest has been captured. A local Docker build or a workflow
definition alone is not release evidence.

## Dependency governance

Dependabot tracks GitHub Actions, Maven modules under `backend/`, and the
soybean-admin frontend dependencies under `frontend-admin/`. The
`MMPay Dependabot Mirror` workflow runs repository dependency contracts and the
secret scan when dependency manifests or dependency workflows change.

## External release boundary

Huifu sandbox credentials, provider private keys, and license signing keys stay
outside this repository. A release cannot be called end-to-end complete until
the external sandbox run and redacted evidence package exist.

The v1.0.0 evidence status is tracked in
`docs/release/external-closure-blockers.md`. Future release evidence items must
remain incomplete until real Huifu, MMMail, registry, operator, and
vendor-issued license relay evidence exists.

Use `docs/release/e2e-evidence-template.md` for the redacted MP-8 evidence file.
After a real sandbox or live run, render the evidence from explicit external
facts:

```bash
MMPAY_EVIDENCE_PROVIDER=huifu \
MMPAY_EVIDENCE_ENVIRONMENT=sandbox \
MMPAY_EVIDENCE_MMMAIL_SHA=<40-char MMMail public release commit SHA> \
MMPAY_EVIDENCE_PROVIDER_EVENT_ID=<redacted provider event ID> \
MMPAY_EVIDENCE_MMMAIL_WEBHOOK_EVENT_ID=<redacted MMMail webhook event ID> \
MMPAY_EVIDENCE_LICENSE_CLAIM_ID=<redacted license claim ID or not-used> \
MMPAY_EVIDENCE_RUN_FINISHED_AT=<ISO 8601 UTC timestamp> \
MMPAY_EVIDENCE_HAPPY=<happy path evidence summary> \
MMPAY_EVIDENCE_BAD_SIGNATURE=<bad signature rejection summary> \
MMPAY_EVIDENCE_EXPIRED_WINDOW=<expired timestamp rejection summary> \
MMPAY_EVIDENCE_PROVIDER_ERROR=<provider error unchanged-state summary> \
MMPAY_EVIDENCE_REPLAY=<duplicate event rejection summary> \
bash scripts/render-e2e-evidence.sh > redacted-e2e-evidence.md
```

The renderer validates its output before writing to stdout. Verify the final
file again with:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
