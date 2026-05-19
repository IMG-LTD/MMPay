#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

PATTERNS=(
  'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY'
  'BEGIN PGP PRIVATE KEY BLOCK'
  'ghp_[A-Za-z0-9_]{20,}'
  'gho_[A-Za-z0-9_]{20,}'
  'sk-[A-Za-z0-9]{20,}'
  'AKIA[0-9A-Z]{16}'
  'license''-signer'
  'generate license signing ''key'
  'self''-sign license'
)

for pattern in "${PATTERNS[@]}"; do
  while IFS= read -r -d '' file; do
    [[ "$file" == "scripts/security-secret-scan.sh" ]] && continue
    [[ -f "$ROOT_DIR/$file" ]] || continue
    if grep -InE "$pattern" "$ROOT_DIR/$file"; then
      echo "security-secret-scan failed: matched pattern $pattern" >&2
      exit 1
    fi
  done < <(git -C "$ROOT_DIR" ls-files --cached --others --exclude-standard -z)
done

echo "security-secret-scan passed"
