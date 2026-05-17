#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/backend/mmpay-gateway-core/src/main/resources/db/migration"

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "No migration directory yet: $MIGRATION_DIR"
  exit 0
fi

bad_names="$(find "$MIGRATION_DIR" -type f ! -name 'V[0-9][0-9][0-9]__*.sql' -print)"
if [[ -n "$bad_names" ]]; then
  echo "$bad_names"
  echo "Migration files must match V001__description.sql" >&2
  exit 1
fi

echo "migration naming check passed"
