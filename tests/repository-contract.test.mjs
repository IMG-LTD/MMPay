import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');

const requiredBackendModules = [
  'mmpay-bom',
  'mmpay-common',
  'mmpay-gateway-core',
  'mmpay-adapter-spi',
  'mmpay-adapter-huifu',
  'mmpay-webhook-out',
  'mmpay-license-relay',
  'mmpay-admin-api',
  'mmpay-app',
];

async function fileExists(relativePath) {
  await access(path.join(root, relativePath), constants.F_OK);
}

describe('MP-1 repository scaffold contract', () => {
  it('defines the Maven backend modules expected by the MMPay spec', async () => {
    await fileExists('backend/pom.xml');

    for (const moduleName of requiredBackendModules) {
      await fileExists(`backend/${moduleName}/pom.xml`);
    }
  });

  it('provides runnable local gates for compile, migration naming, and secret scan', async () => {
    await fileExists('deploy/docker-compose.minimal.yml');
    await fileExists('backend/mmpay-gateway-core/src/main/resources/db/migration/V001__create_payment_core.sql');
    await fileExists('scripts/check-migration-naming.sh');

    const validateLocal = await readFile(path.join(root, 'scripts/validate-local.sh'), 'utf8');

    assert.match(validateLocal, /mvn -f "\$ROOT_DIR\/backend\/pom\.xml" -DskipTests compile/);
    assert.match(validateLocal, /check-migration-naming\.sh/);
    assert.match(validateLocal, /security-secret-scan\.sh/);
  });
});
