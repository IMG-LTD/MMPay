#!/usr/bin/env bash
set -euo pipefail

required=(
  MMPAY_EVIDENCE_PROVIDER
  MMPAY_EVIDENCE_ENVIRONMENT
  MMPAY_EVIDENCE_MMMAIL_SHA
  MMPAY_EVIDENCE_PROVIDER_EVENT_ID
  MMPAY_EVIDENCE_MMMAIL_WEBHOOK_EVENT_ID
  MMPAY_EVIDENCE_LICENSE_CLAIM_ID
)

for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required evidence variable: $name" >&2
    exit 1
  fi
done

if [[ "${MMPAY_EVIDENCE_PROVIDER}" != "huifu" ]]; then
  echo "Unsupported evidence provider: ${MMPAY_EVIDENCE_PROVIDER}" >&2
  exit 1
fi

if [[ "${MMPAY_EVIDENCE_ENVIRONMENT}" != "sandbox" && "${MMPAY_EVIDENCE_ENVIRONMENT}" != "live" ]]; then
  echo "Provider environment must be sandbox or live" >&2
  exit 1
fi

echo "mmpay evidence safety precheck passed"
