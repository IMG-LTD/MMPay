# MMPay v1 RC Cadence

`v1.0.0-rc.N` tags are release candidates for the first GA line. Every
candidate is created from `main` HEAD only.

## Fresh-RC Rule

The GA stream uses fresh-rc per change semantics:

- `v1.0.0-rc.1` is the first GA candidate.
- Any commit after an RC requires a new tag name, for example
  `v1.0.0-rc.2`.
- Never force-tag, delete, or rewrite an existing `v1.*` tag.
- Evidence-only refreshes use an explicit
  `docs/release/rc-N-evidence-refresh` commit and the
  `evidence-refresh-no-op` change class.

Allowed change classes between RCs:

| Class | Scope |
| --- | --- |
| `defect-bound-to-MUSTFIX` | Fixes a release-blocking defect. |
| `evidence-pipeline` | Evidence renderer, verifier, or storage fix. |
| `runbook-prose` | Operator-facing prose without behavior change. |
| `vendor-contract-prose` | Vendor handoff wording or key metadata prose. |
| `cve-only-dep-bump` | Security-only dependency bump with scanner evidence. |
| `evidence-refresh-no-op` | Only `docs/release/*-evidence.md` changes. |

New features, schema migrations, governance scope changes, and non-CVE
dependency upgrades are forbidden between `v1.0.0-rc.N` and `v1.0.0`.

## Promotion

`v1.0.0` is promoted from the latest RC only when:

- image digest evidence is complete and externally verified;
- e2e evidence is complete against `sandbox-with-real-money` or `live`;
- backup-restore drill evidence is signed and no older than 90 days;
- vendor `BINDING_OK` is PGP-signed by a non-expired, non-revoked key;
- `scripts/release-gate.sh --rc` and `scripts/release-gate.sh --ga`
  both pass.
