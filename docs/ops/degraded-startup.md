# Degraded Startup

`MMPAY_FAIL_OPEN_DEGRADED_UI=true` allows the admin UI and forensic
read paths to start when external credential references such as `kms://`
cannot be resolved. It is not a payment-processing mode.

## Blocked In Degraded Mode

The runtime must reject these writes with
`urn:mmpay:problem:degraded-mode-blocked`:

- refunds;
- bulk re-dispatch;
- `/api/admin/evidence/snapshot`;
- `/api/license-relay/v1/forward`.

## Allowed In Degraded Mode

- `/api/admin/audit/verify`;
- credential rebind UI;
- `POST /api/admin/system/clear-degraded` after an operator completes
  credential rebind.

Entering degraded mode emits `system.degraded_mode_entered`. Clearing
degraded mode emits `system.degraded_mode_exited`.
