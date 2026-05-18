#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT_DIR/scripts/security-secret-scan.sh"
bash "$ROOT_DIR/scripts/check-migration-naming.sh"
bash "$ROOT_DIR/scripts/validate-helm-chart.sh"
mvn -f "$ROOT_DIR/backend/pom.xml" -DskipTests compile
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-gateway-core -am -Dtest=PaymentIntentTest,MerchantChannelTest,RefundTest,ReconciliationTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-adapter-huifu -am -Dtest=HuifuAdapterContractTest,HuifuSignedRequestTest,HuifuReconciliationTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-webhook-out -am -Dtest=WebhookOutContractTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-license-relay -am test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-admin-api -am -Dtest=AdminDashboardControllerTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=MmpayApplicationContractTest test
node --test "$ROOT_DIR/tests/repository-contract.test.mjs"
node --test "$ROOT_DIR/tests/e2e-evidence-contract.test.mjs"
pnpm --dir "$ROOT_DIR/frontend-admin" install --frozen-lockfile
pnpm --dir "$ROOT_DIR/frontend-admin" typecheck
pnpm --dir "$ROOT_DIR/frontend-admin" lint
pnpm --dir "$ROOT_DIR/frontend-admin" test
bash -n "$ROOT_DIR/scripts/security-secret-scan.sh"
bash -n "$ROOT_DIR/scripts/check-migration-naming.sh"
bash -n "$ROOT_DIR/scripts/validate-helm-chart.sh"
bash -n "$ROOT_DIR/scripts/validate-e2e-evidence.sh"
bash -n "$ROOT_DIR/scripts/render-e2e-evidence.sh"
bash -n "$ROOT_DIR/scripts/validate-ci.sh"
bash -n "$ROOT_DIR/scripts/validate-local.sh"
bash -n "$ROOT_DIR/scripts/release-gate.sh"

echo "mmpay local validation passed"
