import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');

describe('P2 merchant channel contract', () => {
  it('ships additive gateway migrations V120 through V123 without rewriting V001', async () => {
    const v001 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V001__create_payment_core.sql');
    const v100 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V120__merchants_extend_binding.sql');
    const v101 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V121__channels_extend_binding.sql');
    const v102 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V122__archive_terminal_trigger.sql');
    const v103 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V123__partial_unique_active_id.sql');

    assert.match(v001, /credential_handle VARCHAR\(256\) NOT NULL/);
    assert.match(v100, /credential_ref VARCHAR\(256\) NULL/);
    assert.match(v100, /credential_fingerprint VARCHAR\(8\) NULL/);
    assert.match(v100, /status VARCHAR\(16\) NOT NULL DEFAULT 'active'/);
    assert.match(v100, /status IN \('active', 'suspended', 'archived'\)/);
    assert.match(v100, /tenant_id VARCHAR\(64\) NOT NULL DEFAULT 'default'/);
    assert.match(v100, /created_at TIMESTAMPTZ NOT NULL DEFAULT now\(\)/);
    assert.match(v100, /UPDATE merchants\s+SET credential_ref = 'env:\/\/LEGACY_PLACEHOLDER'/);
    assert.match(v100, /\^env:\/\/\[A-Z\]\[A-Z0-9_\]\{2,127\}\$/);
    assert.match(v101, /tenant_id VARCHAR\(64\) NOT NULL DEFAULT 'default'/);
    assert.match(v101, /status IN \('active', 'suspended', 'archived'\)/);
    assert.match(v101, /UPDATE channels\s+SET credential_ref = 'env:\/\/LEGACY_PLACEHOLDER'/);
    assert.match(v101, /channels_provider_code_not_reserved/);
    assert.match(v101, /provider_code NOT IN \('none', 'mock', 'dummy'\)/);
    assert.match(v102, /reject_archive_resurrection/);
    assert.match(v102, /USING ERRCODE = '23514'/);
    assert.match(v103, /ADD COLUMN row_uid UUID NOT NULL DEFAULT gen_random_uuid\(\)/);
    assert.match(v103, /DROP CONSTRAINT merchants_pkey/);
    assert.match(v103, /DROP CONSTRAINT channels_pkey/);
    assert.match(v103, /merchants_id_active_uniq/);
    assert.match(v103, /channels_id_active_uniq/);
    assert.match(v103, /WHERE status <> 'archived'/);
  });

  it('wires P2 tests into local validation', async () => {
    const validateLocal = await read('scripts/validate-local.sh');

    assert.match(validateLocal, /p2-merchant-channel-contract\.test\.mjs/);
    assert.match(validateLocal, /ProviderRegistryTest,HuifuProviderDescriptorTest/);
    assert.match(validateLocal, /P2MerchantChannelCrudContractTest/);
    assert.match(validateLocal, /P2MerchantChannelLifecycleContractTest/);
  });

  it('ships real backend and Soybean Admin merchant/channel pages', async () => {
    const controller = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/merchant/MerchantAdminController.java');
    const service = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/merchant/MerchantAdminService.java');
    const routes = await read('frontend-admin/src/router/elegant/routes.ts');
    const adminApi = await read('frontend-admin/src/service/api/admin.ts');
    const merchants = await read('frontend-admin/src/views/merchants/index.vue');
    const merchantDetail = await read('frontend-admin/src/views/merchants/detail/index.vue');
    const channelDetail = await read('frontend-admin/src/views/channels/detail/index.vue');
    const channelNew = await read('frontend-admin/src/views/merchants/channel-new/index.vue');

    assert.match(controller, /\/api\/admin/);
    assert.match(controller, /\/merchants/);
    assert.match(controller, /@PatchMapping\("\/merchants\/\{id\}"\)/);
    assert.match(controller, /@DeleteMapping\("\/merchants\/\{id\}"\)/);
    assert.match(controller, /@PostMapping\("\/merchants\/\{id\}\/verify-binding"\)/);
    assert.match(controller, /@PatchMapping\("\/channels\/\{id\}"\)/);
    assert.match(controller, /@DeleteMapping\("\/channels\/\{id\}"\)/);
    assert.match(controller, /@PostMapping\("\/channels\/\{id\}\/verify-binding"\)/);
    assert.match(service, /credential_ref\.bind/);
    assert.match(service, /credential_ref\.unbind/);
    assert.match(service, /merchant\.create/);
    assert.match(service, /merchant\.update/);
    assert.match(service, /merchant\.delete/);
    assert.match(routes, /\/merchants\/:id\/channels\/new/);
    assert.match(adminApi, /updateMerchant/);
    assert.match(adminApi, /archiveMerchant/);
    assert.match(adminApi, /verifyMerchantBinding/);
    assert.match(adminApi, /updateChannel/);
    assert.match(adminApi, /archiveChannel/);
    assert.match(adminApi, /verifyChannelBinding/);
    assert.match(merchants, /<NTable/);
    assert.match(merchants, /createMerchant/);
    assert.match(merchantDetail, /updateMerchant/);
    assert.match(merchantDetail, /archiveMerchant/);
    assert.match(merchantDetail, /verifyMerchantBinding/);
    assert.match(channelDetail, /updateChannel/);
    assert.match(channelDetail, /archiveChannel/);
    assert.match(channelDetail, /verifyChannelBinding/);
    assert.match(channelNew, /<NSelect/);
    assert.doesNotMatch(`${merchants}\n${merchantDetail}\n${channelDetail}\n${channelNew}`, /<(textarea|input\s+type="file")/i);
  });
});

function read(relativePath) {
  return readFile(path.join(root, relativePath), 'utf8');
}
