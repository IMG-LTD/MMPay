#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT_DIR/scripts/security-secret-scan.sh"
bash "$ROOT_DIR/scripts/check-migration-naming.sh"
mvn -f "$ROOT_DIR/backend/pom.xml" -DskipTests compile
node --test "$ROOT_DIR/tests/repository-contract.test.mjs"
bash -n "$ROOT_DIR/scripts/security-secret-scan.sh"
bash -n "$ROOT_DIR/scripts/check-migration-naming.sh"
bash -n "$ROOT_DIR/scripts/validate-local.sh"

echo "mmpay local validation passed"
