#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

PATTERNS=(
  'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
  'ghp_[A-Za-z0-9_]{20,}'
  'gho_[A-Za-z0-9_]{20,}'
  'sk-[A-Za-z0-9]{20,}'
  'AKIA[0-9A-Z]{16}'
  'license-signer'
  'generate license signing key'
  'self-sign license'
)

for pattern in "${PATTERNS[@]}"; do
  if grep -RInE "$pattern" "$ROOT_DIR" \
    --exclude-dir=.git \
    --exclude-dir=target \
    --exclude-dir=node_modules \
    --exclude=security-secret-scan.sh; then
    echo "security-secret-scan failed: matched pattern $pattern" >&2
    exit 1
  fi
done

echo "security-secret-scan passed"
