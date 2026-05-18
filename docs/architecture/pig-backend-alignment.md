# Pig Backend Alignment

MMPay backend migration is being aligned to Pig backend foundations from the real upstream project.

| Item | Value |
| --- | --- |
| Upstream | <https://gitee.com/log4j/pig> |
| Branch | `master` |
| Commit | `28ef625701ebe047984661a61589330b9360d43e` |
| Backend-only modules selected | `pig-register`, `pig-gateway`, `pig-auth`, `pig-upms`, `pig-common`, `db` |
| Initially excluded | `pig-visual` UI and unrelated visual tooling |
| Current runtime status | Partial alignment |

## Runtime Boundary

The v0.2.0 runtime keeps the existing MMPay payment-domain modules active while the Pig backend structure is introduced and tracked. This avoids breaking Huifu adapter, webhook, license relay and dashboard contracts during the urgent frontend rescue release.

MMPay must not claim full Pig runtime parity until these boundaries are implemented with tests:

- Pig auth/login/session replaces the current public admin landing route.
- Pig gateway routing owns MMPay API entry and security filters.
- Pig common/upms integration is used by MMPay admin permissions.
- Pig database bootstrap is reconciled with MMPay Flyway migrations.

Until then, public documents should describe the backend state as Pig-aligned or Pig migration in progress, not fully migrated.
