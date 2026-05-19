# Bootstrap Compromise Rotation

This runbook separates MMPay-actionable incidents from vendor-coordinate
incidents. Every path must end with an observable audit row, metric, or
external vendor confirmation.

| Trigger | Class | Success criterion |
| --- | --- | --- |
| Leaked `MMPAY_BOOTSTRAP_ADMIN_PASSWORD_HASH` | MMPay-actionable | `iam.bootstrap.rotated` audit row exists. |
| Leaked `MMPAY_AUDIT_HMAC_KEY` | MMPay-actionable | restore drill completed and segment restart attested. |
| Leaked `MMPAY_EVIDENCE_SIGNING_KEY` | MMPay-actionable | new evidence key published in `governance/cosign-keys.yaml`; last 90 days re-signed. |
| Leaked `MMPAY_AUDIT_RESTORE_ATTESTATION_KEY` | MMPay-actionable | `system.attestation_key_rotated` row emitted under old key and operator PGP. |
| Suspected `audit_event` chain forgery | MMPay-actionable | `/api/admin/audit/verify` result attached to incident. |
| Suspected `mmpay_app_role` privilege escalation | MMPay-actionable | DB role revoked and credentials rotated. |
| Leaked vendor mTLS leaf cert | Vendor-coordinate | relay disabled until vendor revocation and replacement complete. |
| Leaked vendor mTLS CA private key | Vendor-coordinate | vendor CA bundle replaced; old CA refused. |
| Leaked vendor PGP signing key | Vendor-coordinate | `governance/vendor-keys.yaml` updated and old `BINDING_OK` artifacts invalidated. |

Vendor-coordinate incidents are containment and handoff work for MMPay;
MMPay does not rotate vendor-owned private keys.
