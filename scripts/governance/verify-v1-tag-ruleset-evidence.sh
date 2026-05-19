#!/usr/bin/env bash
set -euo pipefail

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

echo "v1 tag ruleset evidence verified"
