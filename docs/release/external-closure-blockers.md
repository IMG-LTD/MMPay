# External Closure Blockers

This register records the remaining external work that blocks MP-8 end-to-end
closure. It is intentionally separate from local validation because these items
require real sandbox or live systems outside this repository.

| Item | Status | Required evidence |
|---|---|---|
| Huifu sandbox payment | blocked | real Huifu sandbox request and callback evidence |
| MMMail webhook acceptance | blocked | MMMail paid state webhook acceptance evidence |
| License relay | blocked | vendor-issued license claim relay evidence |

## Rules

- Do not mark any item complete from local mocks, generated fixtures, or request
  preparation alone.
- Do not store Huifu merchant credentials, private keys, webhook secrets, or
  license signing material in this repository.
- When all required evidence exists, render the redacted evidence file with
  `scripts/render-e2e-evidence.sh` and validate it with
  `scripts/validate-e2e-evidence.sh`.
