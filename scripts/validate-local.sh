#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "$ROOT_DIR/scripts/security-secret-scan.sh"
bash "$ROOT_DIR/scripts/check-migration-naming.sh"
bash "$ROOT_DIR/scripts/validate-helm-chart.sh"
bash "$ROOT_DIR/scripts/governance/brand-neutrality-scan.sh"
bash "$ROOT_DIR/governance/webhook-out-five-field-scan.sh"
node "$ROOT_DIR/governance/relay-static-scan/scan-license-relay.mjs"
mvn -f "$ROOT_DIR/backend/pom.xml" -DskipTests compile
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=AuditFoundationTest,AuditAppendOnlyTest,ActuatorHardeningTest,ReferenceResolverFoundationTest,SetupFoundationTest,P1AdminSecurityContractTest,P1SetupRouteLifecycleTest,P1SetupRateLimitTest,P1BootstrapAdminInitializerTest,P1SetupStartupTokenLogTest,P1SetupSingleFlightTest,P1SetupThymeleafTemplateTest,JdbcAuditEventStoreTest,AuditChainSerializationTest,SpringAuthorizationServerJdbcWiringTest,ServicePrincipalGrantTest,PasswordGrantTokenTest -Dsurefire.failIfNoSpecifiedTests=false test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-adapter-huifu -am -Dtest=ProviderRegistryTest,HuifuProviderDescriptorTest -Dsurefire.failIfNoSpecifiedTests=false test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=P2MerchantChannelCrudContractTest,P2MerchantChannelLifecycleContractTest -Dsurefire.failIfNoSpecifiedTests=false test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=P3PaymentLifecycleContractTest -Dsurefire.failIfNoSpecifiedTests=false test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=P4LicenseRelayIntegrationContractTest -Dsurefire.failIfNoSpecifiedTests=false test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-gateway-core -am -Dtest=PaymentIntentTest,MerchantChannelTest,RefundTest,ReconciliationTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-adapter-huifu -am -Dtest=HuifuAdapterContractTest,HuifuSignedRequestTest,HuifuReconciliationTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-webhook-out -am -Dtest=WebhookOutContractTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-license-relay -am -Dtest=LicenseRelayTest,LicenseRelaySecurityContractTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-admin-api -am -Dtest=AdminDashboardControllerTest test
timeout 60s mvn -f "$ROOT_DIR/backend/pom.xml" -pl mmpay-app -am -Dtest=MmpayApplicationContractTest,ProviderRegistrySpringContextTest test
node --test "$ROOT_DIR/tests/repository-contract.test.mjs"
node --test "$ROOT_DIR/tests/e2e-evidence-contract.test.mjs"
node --test "$ROOT_DIR/tests/p1-foundation-contract.test.mjs"
node --test "$ROOT_DIR/tests/p2-merchant-channel-contract.test.mjs"
node --test "$ROOT_DIR/tests/p3-payment-lifecycle-contract.test.mjs"
node --test "$ROOT_DIR/tests/p4-license-relay-integrations-contract.test.mjs"
pnpm --dir "$ROOT_DIR/frontend-admin" install --frozen-lockfile
pnpm --dir "$ROOT_DIR/frontend-admin" typecheck
pnpm --dir "$ROOT_DIR/frontend-admin" lint
pnpm --dir "$ROOT_DIR/frontend-admin" test
bash -n "$ROOT_DIR/scripts/security-secret-scan.sh"
bash -n "$ROOT_DIR/scripts/check-migration-naming.sh"
bash -n "$ROOT_DIR/scripts/validate-helm-chart.sh"
bash -n "$ROOT_DIR/scripts/validate-e2e-evidence.sh"
bash -n "$ROOT_DIR/scripts/governance/evidence-safety-precheck.sh"
bash -n "$ROOT_DIR/scripts/render-e2e-evidence.sh"
bash -n "$ROOT_DIR/scripts/validate-ci.sh"
bash -n "$ROOT_DIR/scripts/validate-local.sh"
bash -n "$ROOT_DIR/scripts/release-gate.sh"

echo "mmpay local validation passed"
