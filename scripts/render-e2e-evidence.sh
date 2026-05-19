#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required evidence variable: $name" >&2
    exit 1
  fi
}

require_external_facts() {
  local names=(
    MMPAY_EVIDENCE_PROVIDER
    MMPAY_EVIDENCE_ENVIRONMENT
    MMPAY_EVIDENCE_MMMAIL_SHA
    MMPAY_EVIDENCE_PROVIDER_EVENT_ID
    MMPAY_EVIDENCE_MMMAIL_WEBHOOK_EVENT_ID
    MMPAY_EVIDENCE_LICENSE_CLAIM_ID
    MMPAY_EVIDENCE_RUN_FINISHED_AT
    MMPAY_EVIDENCE_HAPPY
    MMPAY_EVIDENCE_BAD_SIGNATURE
    MMPAY_EVIDENCE_EXPIRED_WINDOW
    MMPAY_EVIDENCE_PROVIDER_ERROR
    MMPAY_EVIDENCE_REPLAY
  )
  for name in "${names[@]}"; do
    require_env "$name"
  done
}

render_evidence() {
  local mmpay_sha="$1"
  cat <<EOF
# MMPay MMMail E2E Evidence

Evidence status: completed-external-evidence
Payment provider: ${MMPAY_EVIDENCE_PROVIDER}
Provider environment: ${MMPAY_EVIDENCE_ENVIRONMENT}
MMPay repository commit SHA: ${mmpay_sha}
MMMail public release commit SHA: ${MMPAY_EVIDENCE_MMMAIL_SHA}
Provider event ID: ${MMPAY_EVIDENCE_PROVIDER_EVENT_ID}
MMMail webhook event ID: ${MMPAY_EVIDENCE_MMMAIL_WEBHOOK_EVENT_ID}
License claim ID: ${MMPAY_EVIDENCE_LICENSE_CLAIM_ID}
Run finished at: ${MMPAY_EVIDENCE_RUN_FINISHED_AT}

## Scenario Evidence

happy: ${MMPAY_EVIDENCE_HAPPY}
bad-signature: ${MMPAY_EVIDENCE_BAD_SIGNATURE}
expired-window: ${MMPAY_EVIDENCE_EXPIRED_WINDOW}
provider-error: ${MMPAY_EVIDENCE_PROVIDER_ERROR}
replay: ${MMPAY_EVIDENCE_REPLAY}

## Boundary Notes

- This evidence file was rendered from external sandbox-with-real-money or live
  run facts.
- It must not contain merchant credentials, provider private keys, webhook
  secrets, or license signing private keys.
EOF
}

require_external_facts
bash "$ROOT_DIR/scripts/governance/evidence-safety-precheck.sh" >&2
mmpay_sha="$(git -C "$ROOT_DIR" rev-parse HEAD)"
tmp_file="$(mktemp)"
trap 'rm -f "$tmp_file"' EXIT
render_evidence "$mmpay_sha" > "$tmp_file"
bash "$ROOT_DIR/scripts/validate-e2e-evidence.sh" "$tmp_file" >/dev/null
cat "$tmp_file"
