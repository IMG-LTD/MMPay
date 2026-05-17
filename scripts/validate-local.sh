#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT_DIR/scripts/security-secret-scan.sh"
bash "$ROOT_DIR/scripts/check-migration-naming.sh"
mvn -f "$ROOT_DIR/backend/pom.xml" -DskipTests compile
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-admin-api -am -Dtest=AdminDashboardControllerTest test
node --test "$ROOT_DIR/tests/repository-contract.test.mjs"
pnpm --dir "$ROOT_DIR/frontend-admin" install --frozen-lockfile
pnpm --dir "$ROOT_DIR/frontend-admin" typecheck
pnpm --dir "$ROOT_DIR/frontend-admin" lint
pnpm --dir "$ROOT_DIR/frontend-admin" test
bash -n "$ROOT_DIR/scripts/security-secret-scan.sh"
bash -n "$ROOT_DIR/scripts/check-migration-naming.sh"
bash -n "$ROOT_DIR/scripts/validate-local.sh"

echo "mmpay local validation passed"
