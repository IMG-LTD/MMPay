# Huifu Provider

Huifu is the first planned provider adapter. Sandbox credentials must be
provided through environment variables, secret files, or an external secret
manager.

No Huifu merchant ID, private key, certificate, or sandbox token may be stored
in this repository.

## dg-payment-skills Boundary

`huifurepo/dg-payment-skills` is review-only guidance for Huifu integration
research. Its repository declares CC BY-NC 4.0 and Huifu-owned technical
content, so its contents must not be compiled into MMPay, vendored, copied into
source, or redistributed in release artifacts.

The current reviewed facts are:

- V2 API envelopes contain `sys_id`, `product_id`, `data`, and `sign`.
- Request signing uses `SHA256withRSA` over first-level sorted `data`
  fields. Interface `notify_url` callbacks verify the raw `resp_data` body with
  the Huifu RSA public key.
- `jpt-x-skill-source` must carry the configured skill source value, and
  `jpt-x-skill-huifu_id` must carry the current request `data.huifu_id` when
  present.
- Console Webhook endpoint keys are not RSA keys; they are only for the
  separate MD5 webhook signing path.
- Runtime credentials remain outside source control.
- Runtime status is currently `available=false` with reason
  `live provider client is not wired`.
- Provider failures must surface explicitly and must not become mock success.
- Invoice integration is currently `unsupported by current provider` until a
  Huifu invoice API path is reviewed and wired with real sandbox evidence.

MMPay may use this guidance to shape adapter contracts, but production Huifu
HTTP or SDK wiring requires a separate license and merchant credential review
before adding any runtime dependency.

## Local Sandbox Variables

The adapter can prepare signed requests and verify `notify_url` payloads from
environment-sourced values:

- `HUIFU_SYS_ID`
- `HUIFU_PRODUCT_ID`
- `HUIFU_RSA_PUBLIC_KEY`
- `HUIFU_RSA_PRIVATE_KEY`
- `HUIFU_SKILL_SOURCE`
- `HUIFU_MERCHANT_ID`
- `HUIFU_NOTIFY_URL`
- `HUIFU_WEBHOOK_ENDPOINT_KEY`
- `HUIFU_SDK_ROOT` optional, only used when an approved local SDK is mounted
