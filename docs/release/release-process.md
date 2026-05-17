# Release Process

MMPay release automation has a local gate and a GHCR image publishing workflow.
Do not publish a release from an unvalidated working tree.

## Local release candidate gate

Run the full local gate before creating a tag or dispatching image publishing:

```bash
bash scripts/validate-local.sh
```

This gate covers secret scanning, migration naming, backend compilation, the
admin dashboard API contract, repository governance contracts, and the
soybean-admin frontend checks.

## Image publishing

The `MMPay Images` workflow builds the root `Dockerfile` and publishes:

```text
ghcr.io/img-ltd/mmpay-app:<git-ref>
ghcr.io/img-ltd/mmpay-app:<commit-sha>
```

Tags are only evidence after the remote workflow has completed successfully and
the immutable image digest has been captured. A local Docker build or a workflow
definition alone is not release evidence.

## External release boundary

Huifu sandbox credentials, provider private keys, and license signing keys stay
outside this repository. A release cannot be called end-to-end complete until the
external sandbox run and redacted evidence package exist.

Use `docs/release/e2e-evidence-template.md` for the redacted MP-8 evidence file,
then verify it with:

```bash
bash scripts/validate-e2e-evidence.sh <redacted-e2e-evidence.md>
```
