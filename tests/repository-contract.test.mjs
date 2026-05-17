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
    assert.match(validateLocal, /AdminDashboardControllerTest/);
    assert.match(validateLocal, /check-migration-naming\.sh/);
    assert.match(validateLocal, /security-secret-scan\.sh/);
  });
});

describe('MMPay open-source framework contract', () => {
  it('anchors the backend on the Pig Spring Cloud Alibaba foundation', async () => {
    const backendPom = await readFile(path.join(root, 'backend/pom.xml'), 'utf8');
    const appPom = await readFile(path.join(root, 'backend/mmpay-app/pom.xml'), 'utf8');
    const appClass = await readFile(
      path.join(root, 'backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/MmpayApplication.java'),
      'utf8',
    );
    const appConfig = await readFile(
      path.join(root, 'backend/mmpay-app/src/main/resources/application.yml'),
      'utf8',
    );
    const readme = await readFile(path.join(root, 'README.md'), 'utf8');

    assert.match(backendPom, /spring-boot-dependencies/);
    assert.match(backendPom, /spring-cloud-dependencies/);
    assert.match(backendPom, /spring-cloud-alibaba-dependencies/);
    assert.match(appPom, /spring-boot-starter-web/);
    assert.match(appPom, /spring-boot-starter-actuator/);
    assert.match(appPom, /spring-cloud-starter-alibaba-nacos-discovery/);
    assert.match(appClass, /@SpringBootApplication/);
    assert.match(appConfig, /spring:\n  application:\n    name: mmpay-app/);
    assert.match(readme, /Pig \(Spring Cloud Alibaba\)/);
    assert.match(readme, /Disabled Pig modules/);
  });

  it('anchors the admin frontend on the soybean-admin stack', async () => {
    const packageJson = JSON.parse(await readFile(path.join(root, 'frontend-admin/package.json'), 'utf8'));
    const mainTs = await readFile(path.join(root, 'frontend-admin/src/main.ts'), 'utf8');
    const notice = await readFile(path.join(root, 'NOTICE'), 'utf8');

    assert.equal(packageJson.dependencies.vue, '^3.5.13');
    assert.equal(packageJson.dependencies.pinia, '^2.3.1');
    assert.equal(packageJson.dependencies['naive-ui'], '^2.44.1');
    assert.equal(packageJson.dependencies['@vueuse/core'], '^12.8.2');
    assert.equal(packageJson.devDependencies.vite, '^7.3.2');
    assert.equal(packageJson.devDependencies['@vitejs/plugin-vue'], '^6.0.7');
    assert.match(mainTs, /from 'vue'/);
    assert.match(mainTs, /from 'pinia'/);
    assert.match(mainTs, /from 'naive-ui'/);
    assert.match(mainTs, /soybean-admin/);
    assert.match(notice, /Pig/);
    assert.match(notice, /soybean-admin/);
  });

  it('documents dg-payment-skills as review-only Huifu guidance', async () => {
    const notice = await readFile(path.join(root, 'NOTICE'), 'utf8');
    const huifuDoc = await readFile(path.join(root, 'docs/providers/huifu.md'), 'utf8');
    const huifuPom = await readFile(path.join(root, 'backend/mmpay-adapter-huifu/pom.xml'), 'utf8');

    assert.match(notice, /dg-payment-skills/);
    assert.match(notice, /CC BY-NC 4\.0/);
    assert.match(huifuDoc, /review-only guidance/);
    assert.match(huifuDoc, /must not be compiled into MMPay/);
    assert.doesNotMatch(huifuPom, /dg-java-sdk/);
  });

  it('provides a root Docker build path for the MMPay app image', async () => {
    const dockerfile = await readFile(path.join(root, 'Dockerfile'), 'utf8');
    const appPom = await readFile(path.join(root, 'backend/mmpay-app/pom.xml'), 'utf8');

    assert.match(dockerfile, /FROM maven:3\.9\.9-eclipse-temurin-21 AS backend-build/);
    assert.match(dockerfile, /FROM eclipse-temurin:21-jre/);
    assert.match(dockerfile, /mvn -f backend\/pom\.xml -pl mmpay-app -am -DskipTests package/);
    assert.match(dockerfile, /USER mmpay/);
    assert.match(appPom, /spring-boot-maven-plugin/);
    assert.match(appPom, /<version>\$\{spring\.boot\.version\}<\/version>/);
    assert.match(appPom, /<goal>repackage<\/goal>/);
  });

  it('defines an image publishing workflow for the Docker build path', async () => {
    const workflow = await readFile(path.join(root, '.github/workflows/images.yml'), 'utf8');
    const imageDocs = await readFile(path.join(root, 'docs/release/image-publishing.md'), 'utf8');
    const releaseProcess = await readFile(path.join(root, 'docs/release/release-process.md'), 'utf8');

    assert.match(workflow, /name: MMPay Images/);
    assert.match(workflow, /docker\/build-push-action@v6/);
    assert.match(workflow, /file: Dockerfile/);
    assert.match(workflow, /ghcr\.io\/img-ltd\/mmpay-app/);
    assert.match(imageDocs, /ghcr\.io\/img-ltd\/mmpay-app/);
    assert.match(releaseProcess, /MMPay Images/);
    assert.match(releaseProcess, /bash scripts\/validate-local\.sh/);
  });
});
