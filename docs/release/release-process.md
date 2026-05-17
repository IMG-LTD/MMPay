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
soybean-admin frontend checks.

## GitHub Release workflow

The `MMPay Release` workflow runs `bash scripts/release-gate.sh` before it
creates a GitHub Release. It only publishes an existing `v*` tag and uses
`gh release create --verify-tag`, so a missing or mistyped tag fails explicitly.

## Image publishing

The `MMPay Images` workflow builds the root `Dockerfile` and publishes:

```text
ghcr.io/img-ltd/mmpay-app:<git-ref>
ghcr.io/img-ltd/mmpay-app:<commit-sha>
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
outside this repository. A release cannot be called end-to-end complete until the
external sandbox run and redacted evidence package exist.

Use `docs/release/e2e-evidence-template.md` for the redacted MP-8 evidence file,
then verify it with:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
