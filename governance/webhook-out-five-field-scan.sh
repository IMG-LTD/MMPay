#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PATTERN='"(edition|seats|features|issuedAt|expiresAt)"'

if grep -RInE "$PATTERN" "$ROOT_DIR/backend" \
  --exclude-dir=target \
  --exclude='LicenseClaimFieldNames.java'; then
  echo "webhook-out five-field scan failed" >&2
  exit 1
fi

echo "webhook-out five-field scan passed"
