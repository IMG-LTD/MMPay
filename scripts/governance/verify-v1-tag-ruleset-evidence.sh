#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <v1-tag-ruleset-evidence.md>" >&2
  exit 2
fi

EVIDENCE_FILE="$1"

if [[ ! -f "$EVIDENCE_FILE" ]]; then
  echo "v1 tag ruleset evidence missing: $EVIDENCE_FILE" >&2
  exit 1
fi

field() {
  local name="$1"
  sed -n "s/^${name}: //p" "$EVIDENCE_FILE" | head -n 1
}

require_field() {
  local name="$1"
  local value
  value="$(field "$name")"
  if [[ -z "$value" ]]; then
    echo "missing v1 tag ruleset evidence field: $name" >&2
    exit 1
  fi
  printf '%s' "$value"
}

require_exact() {
  local name="$1"
  local expected="$2"
  local actual
  actual="$(require_field "$name")"
  if [[ "$actual" != "$expected" ]]; then
    echo "invalid v1 tag ruleset evidence field: $name" >&2
    exit 1
  fi
}

reject_placeholders() {
  if grep -Eiq 'mock|dummy|fake|replace-with|TBD|TODO|FIXME|XXX|<[^>]+>' "$EVIDENCE_FILE"; then
    echo "v1 tag ruleset evidence contains placeholder or fake markers" >&2
    exit 1
  fi
}

verify_ruleset_hash() {
  local expected actual
  expected="$(require_field "Ruleset source sha256")"
  actual="$(sha256sum "$ROOT_DIR/governance/github-rulesets/v1-tags.json" | awk '{print $1}')"
  if [[ "$expected" != "$actual" ]]; then
    echo "Ruleset source sha256 does not match governance/github-rulesets/v1-tags.json" >&2
    exit 1
  fi
}

# Live GitHub Rulesets API query. Opt-in via MMPAY_RULESET_LIVE_CHECK=true; falls back to the
# committed-evidence proof model when off. Spec §1.1.1 makes the API call binding when wired.
verify_ruleset_live_api() {
  if [[ "${MMPAY_RULESET_LIVE_CHECK:-false}" != "true" ]]; then
    return 0
  fi
  if ! command -v gh >/dev/null 2>&1; then
    echo "MMPAY_RULESET_LIVE_CHECK=true requires gh on PATH" >&2
    exit 78
  fi
  # List rulesets for the repository and find one targeting refs/tags/v1.*.
  local rulesets
  rulesets="$(gh api repos/IMG-LTD/MMPay/rulesets --jq '.[].id' 2>/dev/null || true)"
  if [[ -z "$rulesets" ]]; then
    echo "no rulesets returned for IMG-LTD/MMPay" >&2
    exit 1
  fi
  local found_v1_tag=0
  for ruleset_id in $rulesets; do
    local detail
    detail="$(gh api "repos/IMG-LTD/MMPay/rulesets/$ruleset_id" 2>/dev/null || true)"
    if grep -q 'refs/tags/v1' <<<"$detail" && grep -q '"enforcement":"active"' <<<"$detail"; then
      # bypass_actors should be empty per spec §1.1.1
      local bypass
      bypass="$(jq -c '.bypass_actors // []' <<<"$detail" 2>/dev/null || echo '[]')"
      if [[ "$bypass" != "[]" ]]; then
        echo "v1 tag ruleset has non-empty bypass_actors: $bypass" >&2
        exit 1
      fi
      found_v1_tag=1
      break
    fi
  done
  if [[ $found_v1_tag -ne 1 ]]; then
    echo "no active ruleset targeting refs/tags/v1.* found on remote" >&2
    exit 1
  fi
  echo "GitHub Rulesets API confirms v1-tags ruleset active with empty bypass actors"
}

reject_placeholders
require_exact "Evidence status" "completed-external-evidence"
require_exact "Repository" "IMG-LTD/MMPay"
require_exact "Ruleset source" "governance/github-rulesets/v1-tags.json"
require_exact "Ruleset target" "refs/tags/v1.*"
require_exact "Ruleset enforcement" "active"
require_exact "Deletion protection" "enabled"
require_exact "Non-fast-forward protection" "enabled"
require_exact "Creation protection" "enabled"
require_exact "Bypass actors" "none"
require_field "Remote ruleset evidence URL" >/dev/null
require_field "Remote ruleset observed at" >/dev/null
verify_ruleset_hash
verify_ruleset_live_api

echo "v1 tag ruleset evidence verified"
