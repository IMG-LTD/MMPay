#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
bad_names="$(find "$ROOT_DIR/backend" -path '*/src/main/resources/db/migration/*' -type f ! -name 'V[0-9][0-9][0-9]__*.sql' -print)"
if [[ -n "$bad_names" ]]; then
  echo "$bad_names"
  echo "Migration files must match V001__description.sql" >&2
  exit 1
fi

duplicates="$(
  find "$ROOT_DIR/backend" -path '*/src/main/resources/db/migration/*' -type f -name 'V[0-9][0-9][0-9]__*.sql' -printf '%f %p\n' |
    sed -E 's/^V([0-9]+)__.*$/\1 &/' |
    sort |
    awk '
      seen[$1] {
        print seen[$1]
        print $0
      }
      !seen[$1] {
        seen[$1] = $0
      }
    '
)"
if [[ -n "$duplicates" ]]; then
  echo "$duplicates"
  echo "Migration versions must be globally unique across runtime Flyway locations" >&2
  exit 1
fi

echo "migration naming check passed"
