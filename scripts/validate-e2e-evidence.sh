#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <redacted-e2e-evidence.md>" >&2
  exit 2
fi

EVIDENCE_FILE="$1"

if [[ ! -f "$EVIDENCE_FILE" ]]; then
  echo "Evidence file does not exist: $EVIDENCE_FILE" >&2
  exit 1
fi

require_marker() {
  local marker="$1"
  if ! grep -Fq "$marker" "$EVIDENCE_FILE"; then
    echo "Missing required marker: $marker" >&2
    exit 1
  fi
}

require_pattern() {
  local description="$1"
  local pattern="$2"
  if ! grep -Eq "$pattern" "$EVIDENCE_FILE"; then
    echo "Missing or invalid $description" >&2
    exit 1
  fi
}

reject_pattern() {
  local description="$1"
  local pattern="$2"
  if grep -Eiq "$pattern" "$EVIDENCE_FILE"; then
    echo "$description" >&2
    exit 1
  fi
}

require_marker "Evidence status: completed-external-evidence"
require_marker "Payment provider: huifu"
require_marker "Provider event ID:"
require_marker "MMMail webhook event ID:"
require_marker "License claim ID:"
require_marker "Run finished at:"
require_marker "happy:"
require_marker "bad-signature:"
require_marker "expired-window:"
require_marker "provider-error:"
require_marker "replay:"

require_pattern "MMPay repository commit SHA" '^MMPay repository commit SHA: [0-9a-f]{40}$'
require_pattern "MMMail public release commit SHA" '^MMMail public release commit SHA: [0-9a-f]{40}$'
require_pattern "Run finished at timestamp" '^Run finished at: [0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$'

if ! grep -Eq '^Provider environment: (sandbox-with-real-money|live)$' "$EVIDENCE_FILE"; then
  echo "Provider environment must be sandbox-with-real-money or live" >&2
  exit 1
fi

reject_pattern "Evidence still contains placeholder values" '<[^>]+>|replace-with|TBD|TODO'
reject_pattern "Evidence must not use mock, fake, or none provider modes" 'Provider environment: *(mock|fake|none)|Payment provider: *(mock|fake|none)'
reject_pattern "Evidence must not contain secret material" '(api[_-]?key|private[_-]?key|webhook[_-]?secret|merchant[_-]?secret)[=:]'

echo "mmpay e2e evidence markers are complete"
