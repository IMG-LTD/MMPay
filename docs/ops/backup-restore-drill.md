# Backup Restore Drill

MMPay v1.0.0 GA requires a fresh backup-restore drill. A drill older
than 90 days at the candidate RC commit date is not valid evidence.

## Scope

Back up and restore:

- Postgres `pg_basebackup` and WAL archive;
- audit HMAC key;
- `MMPAY_AUDIT_RESTORE_ATTESTATION_KEY`;
- vendor mTLS CA bundle;
- `MMPAY_EVIDENCE_SIGNING_KEY`.

## Procedure

1. Restore the latest backup into a clean Postgres 15 instance.
2. Import the audit HMAC key and restore attestation key from sealed
   escrow.
3. Commit a fresh nonce under `governance/restore-nonces/<uuid>.txt`.
4. Boot with `MMPAY_RESTORE_MODE=true`.
5. Confirm the runtime writes a `system.restore.complete` audit row.
6. Remove `MMPAY_AUDIT_RESTORE_ATTESTATION_KEY` and restart in steady
   state.
7. Run `bash scripts/governance/audit-chain-verify-cli.sh --from 1 --to last`.
8. Render redacted e2e evidence with `scripts/render-e2e-evidence.sh`.
9. Sign the drill evidence with `MMPAY_EVIDENCE_SIGNING_KEY` and an
   operator PGP key listed in `governance/operator-keys.yaml`.

## RTO

The target RTO is 8 hours from backup ingest to signed drill evidence.
