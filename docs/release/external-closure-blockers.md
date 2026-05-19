# External Closure Blockers

This register records the remaining external work that blocks MP-8 end-to-end
closure and `v1.0.0` GA promotion. These items are intentionally separate from
local validation because they require real sandbox, live, registry, vendor, or
upstream repository systems outside this source tree.

## Blocking Register

| ID | Item | Status | Gate impact | Required artifact or proof | Verification path |
|---|---|---|---|---|---|
| MP8-HUIFU-E2E | Huifu payment and callback evidence | blocked-external | release-gate-blocking | `docs/release/v1.0.0-e2e-evidence.md` generated from `sandbox-with-real-money` or `live` provider execution | `scripts/validate-e2e-evidence.sh` |
| MP8-IMAGE-DIGEST | Immutable image digest, registry, cosign, and SLSA evidence | blocked-external | release-gate-blocking | `docs/release/v1.0.0-image-digest-evidence.md` with immutable digest, registry immutability proof, cosign key, and build attestation | `scripts/governance/verify-image-digest-evidence.sh` |
| MP8-BACKUP-RESTORE | Backup-restore drill | blocked-external | release-gate-blocking | `docs/release/backup-restore-drill-evidence.md` with `Evidence status: completed-external-evidence` and valid operator PGP proof | `scripts/release-gate.sh --ga` |
| MP8-VENDOR-BINDING | Vendor license relay binding approval | blocked-external | release-gate-blocking | `docs/release/vendor-binding/v1.0.0-BINDING_OK.asc` signed by the vendor key pinned in `governance/vendor-keys.yaml` | `scripts/release-gate.sh --ga` |
| MP8-UPSTREAM-FLIP | MMMail companion flip proof | blocked-external | upstream-flip-blocking | `governance/mmpay-binding.yaml` in the upstream MMMail repository, pinned to the MMPay `v1.0.0` GA SHA and re-validating all GA evidence files | `docs/integrations/upstream-evidence-flip-contract.md` |
| MP8-V1-TAG-RULESET | Remote v1 tag immutability proof | blocked-external | release-gate-blocking | `docs/release/v1.0.0-v1-tag-ruleset-evidence.md` proving `governance/github-rulesets/v1-tags.json` is installed and v1 tag deletion/non-fast-forward updates are denied | `scripts/governance/verify-v1-tag-ruleset-evidence.sh` |

## Legacy MP-8 Evidence Anchors

These phrases are kept as compatibility anchors for older repository contracts
while the blocking register above provides the authoritative file-level map:

- real Huifu sandbox request and callback evidence
- MMMail paid state webhook acceptance evidence
- vendor-issued license claim relay evidence

## Rules

- Do not mark any item complete from local mocks, generated fixtures, request
  preparation, or self-signed placeholder evidence.
- Do not store Huifu merchant credentials, provider private keys, webhook
  secrets, vendor signing keys, or license signing material in this repository.
- `docs/release/v1.0.0-e2e-evidence.md` must be rendered only after collecting
  real external sandbox-with-real-money or live facts. Render with
  `scripts/render-e2e-evidence.sh` and validate with
  `scripts/validate-e2e-evidence.sh`.
- `docs/release/v1.0.0-image-digest-evidence.md` must reference the immutable
  `v1.0.0` image digest, not a preview tag digest.
- The GA release gate must continue to fail while any `release-gate-blocking`
  item remains `blocked-external`.
- The upstream MMMail companion flip must remain disabled while any
  `upstream-flip-blocking` item remains `blocked-external`.
