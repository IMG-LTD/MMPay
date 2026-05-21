# Runbook

This index covers the v1.0.0 operator surface and the `v1.0.0-hotfix.1`
post-GA fixes. It does not replace external Huifu, upstream webhook, or vendor
license relay evidence collection.

| Topic | Document | Gate |
| --- | --- | --- |
| Install | `docs/ops/install.md` | `bash scripts/validate-helm-chart.sh` |
| Upgrade | `docs/ops/upgrade.md` | `bash scripts/check-migration-naming.sh` |
| Backup / restore | `docs/ops/backup-restore.md` | operator drill |
| Backup-restore drill | `docs/ops/backup-restore-drill.md` | signed drill evidence |
| Bootstrap compromise | `docs/ops/bootstrap-compromise-rotation.md` | incident audit row |
| Degraded startup | `docs/ops/degraded-startup.md` | degraded-mode route tests |
| Evidence generation | `docs/release/e2e-evidence-template.md` | `bash scripts/validate-e2e-evidence.sh` |
| Image digest evidence | `docs/release/image-digest-evidence-template.md` | `bash scripts/governance/verify-image-digest-evidence.sh` |
| RC cadence | `docs/release/rc-cadence.md` | `bash scripts/release-gate.sh --rc` |
| GA promotion | `docs/release/v1.0.0-release-notes.md` | `bash scripts/release-gate.sh --ga` |
| Hotfix release | `docs/release/v1.0.0-hotfix.1-release-notes.md` | `bash scripts/governance/verify-image-digest-evidence.sh` |
| Deprecation | `docs/governance/deprecation-policy.md` | repository contract tests |
| Support boundary | `SUPPORT.md` | maintainer review |

## Health Check

```bash
curl -fsS http://localhost:8080/
curl -fsS http://localhost:8080/actuator/health
```

The first command returns the bundled admin UI. The second returns the actuator
health response.

## Admin Dashboard State

Before real Huifu credentials are connected, `/api/admin/dashboard` reports
channel status as `credentials-required`. Do not seed fake paid rows or fake
provider success to make the dashboard look complete.

## Evidence Boundary

Keep GA blocked until all external evidence files are present and verified.
Missing evidence must fail the release gate explicitly.
