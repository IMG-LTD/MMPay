# MMPay MMMail E2E Evidence

Evidence status: completed-external-evidence
Payment provider: huifu
Provider environment: sandbox
MMPay repository commit SHA: <40-char MMPay commit SHA>
MMMail public release commit SHA: <40-char MMMail v2.2.0-public commit SHA>
Provider event ID: <redacted Huifu sandbox provider event ID>
MMMail webhook event ID: <redacted MMMail webhook event ID>
License claim ID: <redacted vendor-issued license claim ID, or not-used>
Run finished at: <ISO 8601 UTC timestamp>

## Scenario Evidence

happy: <MMMail order -> MMPay -> Huifu sandbox -> MMPay webhook -> MMMail paid event evidence>
bad-signature: <explicit MMMail signature failure evidence; paid state unchanged>
expired-window: <explicit timestamp window rejection evidence; paid state unchanged>
provider-error: <provider 500 or disconnect evidence; paid state unchanged>
replay: <duplicate event ID evidence; duplicate rejected and state unchanged>

## Boundary Notes

- Do not include merchant credentials, provider private keys, webhook secrets, or
  license signing private keys.
- The license issuer remains outside MMMail and MMPay. This file records only a
  redacted license claim ID when the relay path is exercised.
- This template is not evidence until every placeholder is replaced with real
  redacted values from an actual sandbox or live run.

## Rendering Helper

Prefer rendering the final file through `scripts/render-e2e-evidence.sh` after
collecting the real external run facts. The helper requires all scenario fields
as environment variables and runs `scripts/validate-e2e-evidence.sh` before it
prints the completed evidence document.
