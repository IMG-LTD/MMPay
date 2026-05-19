import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { describe, it } from 'node:test';

const read = path => readFile(new URL(`../${path}`, import.meta.url), 'utf8');
const exists = path => existsSync(new URL(`../${path}`, import.meta.url));

describe('P4 license relay and integrations contract', () => {
  it('ships upgrade-safe relay migrations after V109', async () => {
    const v110 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V110__license_relay_targets.sql');
    const v111 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V111__license_relay_logs.sql');
    const v112 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V112__webhook_integrations_kind_slug.sql');
    const v113 = await read('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V113__license_relay_append_only.sql');

    assert.match(v110, /CREATE TABLE IF NOT EXISTS license_relay_targets/);
    assert.match(v110, /status IN \('active','suspended','archived'\)/);
    assert.match(v111, /CREATE TABLE IF NOT EXISTS license_relay_logs/);
    assert.match(v111, /payload_sha256 CHAR\(64\) NOT NULL/);
    assert.match(v112, /kind VARCHAR\(16\)/);
    assert.match(v112, /slug VARCHAR\(64\)/);
    assert.match(v113, /REVOKE UPDATE, DELETE, TRUNCATE ON license_relay_logs/);
  });

  it('keeps mmpay-license-relay inside the opaque-byte dependency boundary', async () => {
    const pom = await read('backend/mmpay-license-relay/pom.xml');
    const relay = await read('backend/mmpay-license-relay/src/main/java/com/imgltd/mmpay/license/LicenseRelay.java');
    const source = relay + '\n' + await read('backend/mmpay-license-relay/src/main/java/com/imgltd/mmpay/license/LicenseRelayReceipt.java');

    assert.doesNotMatch(pom, /jackson|gson|snakeyaml|protobuf|okhttp|httpclient|spring-web|spring-aop|reactor/i);
    assert.match(relay, /java\.net\.http\.HttpClient/);
    assert.match(relay, /MessageDigest\.getInstance\("SHA-256"\)/);
    assert.doesNotMatch(source, /ObjectMapper|JsonNode|Gson|Yaml|Files\.write|System\.loadLibrary|Runtime\.getRuntime\(\)\.load/);
  });

  it('wires P4 backend, governance, docs, and validation commands', async () => {
    assert.equal(exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/relay/LicenseRelayController.java'), true);
    assert.equal(exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/integrations/IntegrationAdminController.java'), true);
    assert.equal(exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/evidence/EvidenceSnapshotController.java'), true);
    assert.equal(exists('governance/relay-static-scan/scan-license-relay.mjs'), true);
    assert.equal(exists('governance/relay-test-sha256.txt'), true);
    assert.equal(exists('docs/integrations/license-relay-vendor.md'), true);
    assert.equal(exists('docs/release/preview/v0.6.0-image-digest.md'), true);

    const validate = await read('scripts/validate-local.sh');
    assert.match(validate, /P4LicenseRelayIntegrationContractTest/);
    assert.match(validate, /LicenseRelaySecurityContractTest/);
    assert.match(validate, /p4-license-relay-integrations-contract\.test\.mjs/);
    assert.match(validate, /evidence-safety-precheck\.sh/);
  });

  it('ships data-driven Integrations pages without source-level brand literals', async () => {
    const routes = await read('frontend-admin/src/router/elegant/routes.ts');
    const api = await read('frontend-admin/src/service/api/admin.ts');
    const list = await read('frontend-admin/src/views/integrations/index.vue');
    const detail = await read('frontend-admin/src/views/integrations/detail/index.vue');
    const packageJson = JSON.parse(await read('frontend-admin/package.json'));

    assert.match(packageJson.version, /^0\.(6|7|8)\.\d+$/);
    assert.match(routes, /name: 'integrations'/);
    assert.match(routes, /name: 'integration-detail'/);
    assert.match(api, /fetchIntegrations/);
    assert.match(api, /testIntegration/);
    assert.match(detail, /Relay Activity/);
    assert.match(detail, /redispatchLicenseRelayLog/);
    assert.doesNotMatch(`${routes}\n${list}\n${detail}`, /MMMail|mmmail/);
    assert.doesNotMatch(`${list}\n${detail}`, /v-html|innerHTML|domPropsInnerHTML|RawHTML/);
  });
});
