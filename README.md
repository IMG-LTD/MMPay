# MMPay

MMPay is an independent payment gateway project for self-hosted products. Its
first integration target is MMMail, but the gateway is not part of the MMMail
source tree and must remain in its own repository.

## Status

This repository has the MP-0 through MP-7 scaffold in place:

- Backend foundation: Pig (Spring Cloud Alibaba) compatible Spring Boot app
  baseline, with payment domain modules split under `backend/`.
- Payment domain: payment intent, transaction, refund, reconciliation, and
  Flyway migration contracts.
- Provider adapter: Huifu reconciliation mapping MVP.
- Outbound webhook: MMMail-compatible HMAC signature contract.
- License boundary: relay-only delivery; no license signing module exists here.
- Admin surface: read-only dashboard API plus soybean-admin stack frontend using
  Vue 3, Vite, Pinia, and Naive UI. Merchant, channel, order, refund, webhook,
  and reconciliation views are present; runtime tables remain empty until a real
  provider connection exists. Credential fields display only secret handles.

Disabled Pig modules for this phase: code generation, full auth center,
standalone gateway cluster, distributed job scheduler, and unrelated sample
business modules. MMPay keeps the Pig-style Spring Cloud Alibaba foundation but
only enables the minimal app surface required by the payment gateway.

## Security Boundary

- Merchant credentials, provider private keys, customer secrets, and license
  signing private keys must never be committed.
- MMPay emits payment facts. It does not issue, self-sign, or generate MMMail
  licenses.
- License issuance remains an IMG-LTD vendor-controlled process outside this
  repository.

## Local Validation

```bash
bash scripts/validate-local.sh
```
