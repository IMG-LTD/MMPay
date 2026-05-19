# License Relay Vendor Contract

MMPay accepts vendor-signed license payloads as opaque bytes. The relay does not decode, re-sign, generate, or persist license claims.

## Endpoint

- `POST /api/license-relay/v1/forward`
- `Content-Type: application/octet-stream`
- Required headers:
  - `X-License-Relay-Target-Id`: configured relay target ID
  - `X-Request-Id`: caller idempotency or trace identifier
  - trust header or mTLS client certificate, depending on deployment mode

## Behavior

- Payloads larger than 65536 bytes are rejected with `urn:mmpay:problem:relay-payload-too-large`.
- Missing trust material returns 404 on the admin port so the relay endpoint is not exposed as a normal admin API.
- Successful dispatch records payload SHA-256, upstream HTTP status, response SHA-256, response size, truncation state, and audit action `license_relay.forwarded`.
- Redirects and upstream failures are explicit failures and are recorded with stable `error_class` values.

## Deployment Notes

Production deployments should expose the relay path only through the dedicated mTLS connector or a trusted proxy that injects the configured trust header after validating the client certificate. Admin UI traffic should continue to use the normal admin listener.
