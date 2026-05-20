# Upstream Evidence Flip Contract

This contract defines what an upstream consumer verifies after MMPay
v1.0.0 GA. The upstream repository pins the MMPay GA evidence commit SHA in
`governance/mmpay-binding.yaml` and fetches evidence files through:

```text
gh api repos/IMG-LTD/MMPay/contents/<path>?ref=<MMPay v1.0.0 GA evidence SHA>
```

The upstream verifier re-validates fetched content and does not trust the
fetch alone.

## Assertions

1. `docs/release/v1.0.0-image-digest-evidence.md` exists at the pinned
   SHA with `Evidence status: completed-external-evidence` and
   `tag: v1.0.0`.
2. `docs/release/v1.0.0-e2e-evidence.md` exists at the pinned SHA with
   `Provider environment: sandbox-with-real-money` or
   `Provider environment: live`.
3. `docs/release/backup-restore-drill-evidence.md` exists at the pinned
   SHA with a drill date within 90 days of the GA tag date.
4. `docs/release/vendor-binding/v1.0.0-BINDING_OK.asc` exists at the
   pinned SHA and validates against the vendor key fingerprint pinned in
   `governance/mmpay-binding.yaml`.
