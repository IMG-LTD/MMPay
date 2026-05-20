import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');

describe('P3 payment lifecycle contract', () => {
  it('ships v0.4.0-upgrade-safe payment lifecycle migrations after V103', async () => {
    const v104 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V104__payment_lifecycle_alter.sql');
    const v105 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V105__provider_events.sql');
    const v106 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V106__webhook_integrations.sql');
    const v107 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V107__delivery_logs.sql');
    const v108 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V108__reconciliation_runs.sql');
    const v109 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V109__payment_lifecycle_constraints.sql');

    assert.match(v104, /ADD COLUMN IF NOT EXISTS merchant_id VARCHAR\(64\)/);
    assert.match(v104, /version BIGINT NOT NULL DEFAULT 0/);
    assert.match(v104, /USING created_at AT TIME ZONE 'UTC'/);
    assert.match(v105, /provider_events/);
    assert.match(v105, /raw_signature_sha256 CHAR\(64\) NOT NULL/);
    assert.match(v106, /webhook_integrations/);
    assert.match(v106, /target_url VARCHAR\(2048\) NOT NULL/);
    assert.match(v107, /delivery_logs/);
    assert.match(v107, /dead_letter BOOLEAN NOT NULL DEFAULT FALSE/);
    assert.match(v108, /reconciliation_runs/);
    assert.match(v108, /ack_status VARCHAR\(16\) NOT NULL DEFAULT 'pending'/);
    assert.match(v109, /refund_total_guard/);
    assert.match(v109, /payment_intent_terminal_guard/);
  });

  it('exposes P3 backend endpoints and wires them into local validation', async () => {
    const controller = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/payment/PaymentAdminController.java');
    const service = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/payment/PaymentService.java');
    const verifier = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/payment/ProviderCallbackVerifier.java');
    const validateLocal = await read('scripts/validate-local.sh');

    assert.match(controller, /\/api\/admin\/payment-intents/);
    assert.match(controller, /\/api\/admin\/refunds/);
    assert.match(controller, /\/api\/admin\/reconciliation\/runs/);
    assert.match(controller, /\/api\/admin\/webhook-out\/integrations/);
    assert.match(controller, /\/api\/admin\/webhook-out\/delivery-logs/);
    assert.match(controller, /\/webhook-in\/\{providerCode\}/);
    assert.match(service, /provider-live-calls/);
    assert.match(service, /credential_ref\.mismatch_detected/);
    assert.match(verifier, /MessageDigest\.isEqual/);
    assert.match(validateLocal, /P3PaymentLifecycleContractTest/);
    assert.match(validateLocal, /p3-payment-lifecycle-contract\.test\.mjs/);
  });

  it('ships Soybean Admin pages for the P3 operator workflow', async () => {
    const routes = await read('frontend-admin/src/router/elegant/routes.ts');
    const api = await read('frontend-admin/src/service/api/admin.ts');
    const packageJson = JSON.parse(await read('frontend-admin/package.json'));

    for (const file of [
      'frontend-admin/src/views/payments/index.vue',
      'frontend-admin/src/views/payments/detail/index.vue',
      'frontend-admin/src/views/refunds/index.vue',
      'frontend-admin/src/views/refunds/detail/index.vue',
      'frontend-admin/src/views/refunds/new/index.vue',
      'frontend-admin/src/views/reconciliation/index.vue',
      'frontend-admin/src/views/reconciliation/detail/index.vue',
      'frontend-admin/src/views/webhook-out/index.vue',
      'frontend-admin/src/views/webhook-out/delivery-log-detail/index.vue',
    ]) {
      await exists(file);
    }
    assert.equal(packageJson.version, '1.0.0');
    assert.match(routes, /path: '\/payments'/);
    assert.match(routes, /path: '\/refunds\/new'/);
    assert.match(routes, /path: '\/reconciliation'/);
    assert.match(routes, /path: '\/webhook-out'/);
    assert.match(api, /createPaymentIntent/);
    assert.match(api, /createRefund/);
    assert.match(api, /ackReconciliationRun/);
    assert.match(api, /bulkRedispatch/);
    assert.match(api, /fetchDeliveryLogs/);
    assert.match(api, /redispatchDeliveryLog/);
  });
});

async function exists(relativePath) {
  await access(path.join(root, relativePath), constants.F_OK);
}

function read(relativePath) {
  return readFile(path.join(root, relativePath), 'utf8');
}
