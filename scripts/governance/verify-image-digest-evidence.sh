#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIGEST_RE='^sha256:[0-9a-f]{64}$'

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <image-digest-evidence.md>" >&2
  exit 2
fi

EVIDENCE_FILE="$1"

if [[ ! -f "$EVIDENCE_FILE" ]]; then
  echo "image digest evidence missing: $EVIDENCE_FILE" >&2
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
    echo "missing image digest evidence field: $name" >&2
    exit 1
  fi
  printf '%s' "$value"
}

require_digest() {
  local name="$1"
  local value
  value="$(require_field "$name")"
  if [[ ! "$value" =~ $DIGEST_RE ]]; then
    echo "invalid digest for $name" >&2
    exit 1
  fi
}

reject_placeholders() {
  if grep -Eiq 'mock|dummy|fake|test|replace-with|TBD|TODO|FIXME|XXX|<[^>]+>' "$EVIDENCE_FILE"; then
    echo "image digest evidence contains placeholder or fake markers" >&2
    exit 1
  fi
}

verify_compose_digest() {
  local expected actual
  expected="$(require_field "compose_digest")"
  actual="sha256:$(sha256sum "$ROOT_DIR/deploy/docker-compose.yml" | awk '{print $1}')"
  if [[ "$expected" != "$actual" ]]; then
    echo "compose_digest does not match deploy/docker-compose.yml" >&2
    exit 1
  fi
}

verify_registry_inputs() {
  local registry signing_key
  registry="$(require_field "registry_host")"
  signing_key="$(require_field "signing_key_fingerprint")"
  if grep -Eq '^(localhost|127\.|10\.|172\.(1[6-9]|2[0-9]|3[0-1])\.|192\.168\.)' <<<"$registry"; then
    echo "registry_host must not be loopback or RFC1918" >&2
    exit 1
  fi
  grep -Fq "host: $registry" "$ROOT_DIR/governance/registry-tls-pins.yaml" \
    || { echo "registry TLS pin missing for $registry" >&2; exit 1; }
  grep -Fq "$signing_key" "$ROOT_DIR/governance/cosign-keys.yaml" \
    || { echo "signing key fingerprint not pinned in governance/cosign-keys.yaml" >&2; exit 1; }
  require_field "registry_immutability_proof" >/dev/null
}

verify_workflow_head() {
  local url run_id expected
  url="$(require_field "workflow_run_url")"
  expected="$(require_field "commit_sha")"
  run_id="${url##*/}"
  if command -v gh >/dev/null 2>&1 && [[ "$run_id" =~ ^[0-9]+$ ]]; then
    actual="$(gh run view "$run_id" --repo IMG-LTD/MMPay --json headSha --jq .headSha)"
    if [[ "$actual" != "$expected" ]]; then
      echo "workflow run head SHA does not match evidence commit_sha" >&2
      exit 1
    fi
  else
    echo "gh is required to verify workflow_run_url head SHA" >&2
    exit 1
  fi
}

reject_placeholders
grep -Fq "Evidence status: completed-external-evidence" "$EVIDENCE_FILE" \
  || { echo "image digest evidence status is not completed-external-evidence" >&2; exit 1; }
require_digest "mmpay-app"
require_digest "mmpay-frontend-admin"
require_digest "mmpay-app-debug-symbols"
require_digest "compose_digest"
require_digest "build_attestation_hash"
verify_compose_digest
verify_registry_inputs
verify_workflow_head

echo "image digest evidence verified"
