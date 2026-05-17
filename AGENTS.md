# AGENTS.md

This file defines local agent rules for the MMPay repository.

## Repository Boundary

- MMPay must be cloned as a sibling of MMMail, for example
  `/home/xiang/桌面/project/MMMail-test/MMPay`.
- Do not place MMPay under the MMMail working tree.
- Do not copy MMMail product frontend, backend modules, or repository gates into
  this repository unless a spec explicitly requires a shared contract document.

## License Boundary

- MMPay never implements MMMail license signing.
- Do not add modules, scripts, prompts, docs, UI actions, or tests that create
  signing keys, issue local licenses, or issue MMMail license claims.
- MMPay may relay opaque vendor-signed license bytes in a future module, but it
  must not parse, modify, re-sign, or generate license claims.
- Merchant credentials, provider private keys, payment certificates, webhook
  secrets, and license signing private keys must stay outside source control.

## Engineering Rules

- Failures must be explicit. Do not add silent fallbacks, mock paid states, or
  fake provider success paths.
- Keep functions under 50 lines and active source files under 500 lines unless a
  documented governance exception exists.
- Prefer small modules with clear dependencies. Provider adapters must not own
  gateway state.
- Run `bash scripts/validate-local.sh` before claiming a change is complete.
