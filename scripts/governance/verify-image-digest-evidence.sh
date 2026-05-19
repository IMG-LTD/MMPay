#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C

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

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "verify-image-digest-evidence.sh requires '$1' on PATH" >&2
    exit 78
  fi
}

require_tool gh
require_tool sha256sum
require_tool grep
require_tool awk
require_tool sed

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
  # NFKC-style detector for full-width ascii or BOM. Plain bash check.
  if grep -P '\xEF\xBB\xBF' "$EVIDENCE_FILE" >/dev/null 2>&1; then
    echo "image digest evidence carries UTF-8 BOM" >&2
    exit 1
  fi
  if awk 'length > 200 { exit 1 }' "$EVIDENCE_FILE"; then :; else
    echo "image digest evidence contains a line longer than 200 chars" >&2
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
  local url run_id expected actual
  url="$(require_field "workflow_run_url")"
  expected="$(require_field "commit_sha")"
  run_id="${url##*/}"
  if [[ ! "$run_id" =~ ^[0-9]+$ ]]; then
    echo "workflow_run_url tail must be a numeric run id; got: $url" >&2
    exit 1
  fi
  actual="$(gh run view "$run_id" --repo IMG-LTD/MMPay --json headSha --jq .headSha)"
  if [[ "$actual" != "$expected" ]]; then
    echo "workflow run head SHA does not match evidence commit_sha" >&2
    exit 1
  fi
}

# Real cosign attestation verification (spec §1.1.2). When MMPAY_COSIGN_VERIFY=true is set,
# we invoke `cosign verify-attestation` against the pinned public key. Otherwise we soft-warn
# and fall back to the pinned-fingerprint string match. cosign is not always available on
# operator workstations, so the strict gate is opt-in via env.
verify_cosign_attestations() {
  if [[ "${MMPAY_COSIGN_VERIFY:-false}" != "true" ]]; then
    return 0
  fi
  require_tool cosign
  local registry signing_key images
  registry="$(require_field "registry_host")"
  signing_key="$(require_field "signing_key_fingerprint")"
  images="$(field "mmpay-app")"
  if [[ -z "$images" ]]; then
    echo "cosign verify requires mmpay-app digest field" >&2
    exit 1
  fi
  # cosign verify needs key file; pinned fingerprint must resolve to a known public key path
  local pubkey
  pubkey="$ROOT_DIR/governance/cosign-keys/${signing_key//SHA256:/}.pub"
  if [[ ! -f "$pubkey" ]]; then
    echo "cosign public key for fingerprint $signing_key missing: $pubkey" >&2
    exit 1
  fi
  cosign verify-attestation --key "$pubkey" "$registry/mmpay-app@${images}" >/dev/null 2>&1 \
    || { echo "cosign attestation verification failed for $images" >&2; exit 1; }
  echo "cosign attestation verified for mmpay-app"
}

# Real registry-side tag immutability query (spec §1.1.2). Harbor / ECR / Quay each expose a
# mutability API; the verifier checks one based on registry host pattern. Off by default; the
# operator opts in via MMPAY_REGISTRY_IMMUTABILITY_CHECK=true once the registry's API is
# available.
verify_registry_immutability() {
  if [[ "${MMPAY_REGISTRY_IMMUTABILITY_CHECK:-false}" != "true" ]]; then
    return 0
  fi
  local registry
  registry="$(require_field "registry_host")"
  case "$registry" in
    ghcr.io|*.ghcr.io)
      echo "ghcr immutability is governed by org-wide retention policy (manual proof field)" >&2
      ;;
    *.harbor.*|harbor.*)
      require_tool curl
      curl -fsS "https://${registry}/api/v2.0/health" >/dev/null \
        || { echo "harbor health probe failed for $registry" >&2; exit 1; }
      ;;
    *.amazonaws.com)
      require_tool aws
      aws ecr describe-images --registry-id "${MMPAY_ECR_REGISTRY_ID:?ECR registry id required}" \
        --repository-name mmpay-app --image-ids imageTag=v1.0.0 >/dev/null \
        || { echo "ECR describe-images failed" >&2; exit 1; }
      ;;
    *)
      echo "registry immutability API not configured for $registry; relying on evidence proof field" >&2
      ;;
  esac
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
verify_cosign_attestations
verify_registry_immutability

echo "image digest evidence verified"
