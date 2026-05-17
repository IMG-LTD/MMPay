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
    await fileExists('deploy/docker-compose.yml');
    await fileExists('deploy/docker-compose.minimal.yml');
    await fileExists('backend/mmpay-gateway-core/src/main/resources/db/migration/V001__create_payment_core.sql');
    await fileExists('scripts/check-migration-naming.sh');
    await fileExists('scripts/validate-ci.sh');
    await fileExists('scripts/release-gate.sh');
    await fileExists('.github/workflows/release.yml');
    await fileExists('.github/workflows/dependabot-mirror.yml');
    await fileExists('.github/dependabot.yml');

    const validateLocal = await readFile(path.join(root, 'scripts/validate-local.sh'), 'utf8');
    const validateCi = await readFile(path.join(root, 'scripts/validate-ci.sh'), 'utf8');
    const releaseGate = await readFile(path.join(root, 'scripts/release-gate.sh'), 'utf8');
    const ciWorkflow = await readFile(path.join(root, '.github/workflows/ci.yml'), 'utf8');
    const releaseWorkflow = await readFile(path.join(root, '.github/workflows/release.yml'), 'utf8');
    const dependabotMirrorWorkflow = await readFile(
      path.join(root, '.github/workflows/dependabot-mirror.yml'),
      'utf8',
    );
    const dependabotConfig = await readFile(path.join(root, '.github/dependabot.yml'), 'utf8');

    assert.match(validateLocal, /mvn -f "\$ROOT_DIR\/backend\/pom\.xml" -DskipTests compile/);
    assert.match(validateLocal, /PaymentIntentTest,RefundTest,ReconciliationTest/);
    assert.match(validateLocal, /HuifuAdapterContractTest,HuifuReconciliationTest/);
    assert.match(validateLocal, /WebhookOutContractTest/);
    assert.match(validateLocal, /mmpay-license-relay/);
    assert.match(validateLocal, /AdminDashboardControllerTest/);
    assert.match(validateLocal, /MmpayApplicationContractTest/);
    assert.match(validateLocal, /e2e-evidence-contract\.test\.mjs/);
    assert.match(validateLocal, /check-migration-naming\.sh/);
    assert.match(validateLocal, /security-secret-scan\.sh/);
    assert.match(validateLocal, /validate-ci\.sh/);
    assert.match(validateLocal, /release-gate\.sh/);
    assert.match(validateCi, /validate-local\.sh/);
    assert.match(releaseGate, /git -C "\$ROOT_DIR" status --short/);
    assert.match(releaseGate, /validate-ci\.sh/);
    assert.match(ciWorkflow, /bash scripts\/validate-ci\.sh/);
    assert.match(releaseWorkflow, /name: MMPay Release/);
    assert.match(releaseWorkflow, /tags:\n      - 'v\*'/);
    assert.match(releaseWorkflow, /bash scripts\/release-gate\.sh/);
    assert.match(releaseWorkflow, /gh release create "\$\{RELEASE_TAG\}"/);
    assert.match(releaseWorkflow, /--verify-tag/);
    assert.match(dependabotMirrorWorkflow, /name: MMPay Dependabot Mirror/);
    assert.match(dependabotMirrorWorkflow, /repository-contract\.test\.mjs/);
    assert.match(dependabotMirrorWorkflow, /security-secret-scan\.sh/);
    assert.match(dependabotConfig, /package-ecosystem: "github-actions"/);
    assert.match(dependabotConfig, /package-ecosystem: "maven"/);
    assert.match(dependabotConfig, /directory: "\/backend"/);
    assert.match(dependabotConfig, /package-ecosystem: "npm"/);
    assert.match(dependabotConfig, /directory: "\/frontend-admin"/);
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
    const compose = await readFile(path.join(root, 'deploy/docker-compose.yml'), 'utf8');
    const minimalCompose = await readFile(path.join(root, 'deploy/docker-compose.minimal.yml'), 'utf8');
    const installDoc = await readFile(path.join(root, 'docs/ops/install.md'), 'utf8');

    assert.match(dockerfile, /FROM maven:3\.9\.9-eclipse-temurin-21 AS backend-build/);
    assert.match(dockerfile, /FROM eclipse-temurin:21-jre/);
    assert.match(dockerfile, /mvn -f backend\/pom\.xml -pl mmpay-app -am -DskipTests package/);
    assert.match(dockerfile, /USER mmpay/);
    assert.match(compose, /"8080:8080"/);
    assert.match(compose, /build:\n      context: \.\./);
    assert.match(compose, /replace-with-huifu-merchant-id/);
    assert.match(minimalCompose, /image: mmpay-app:local/);
    assert.match(installDoc, /docker build -t mmpay-app:local \./);
    assert.match(installDoc, /docker compose -f deploy\/docker-compose\.yml up --build/);
    assert.match(installDoc, /docker compose -f deploy\/docker-compose\.minimal\.yml up/);
    assert.doesNotMatch(installDoc, /not installable yet/);
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
    assert.match(releaseProcess, /MMPay Release/);
    assert.match(releaseProcess, /MMPay Dependabot Mirror/);
    assert.match(releaseProcess, /bash scripts\/validate-local\.sh/);
    assert.match(releaseProcess, /bash scripts\/release-gate\.sh/);
    assert.match(releaseProcess, /gh release create --verify-tag/);
  });

  it('keeps operational docs aligned with the runnable baseline', async () => {
    const overview = await readFile(path.join(root, 'docs/architecture/overview.md'), 'utf8');
    const runbook = await readFile(path.join(root, 'docs/ops/runbook.md'), 'utf8');
    const upgrade = await readFile(path.join(root, 'docs/ops/upgrade.md'), 'utf8');
    const backupRestore = await readFile(path.join(root, 'docs/ops/backup-restore.md'), 'utf8');

    assert.doesNotMatch(overview, /MP-0 repository contains only governance/);
    assert.match(overview, /mmpay-admin-api/);
    assert.match(runbook, /\/actuator\/health/);
    assert.match(runbook, /credentials-required/);
    assert.match(upgrade, /bash scripts\/check-migration-naming\.sh/);
    assert.match(backupRestore, /pg_dump/);
    assert.match(backupRestore, /pg_restore/);
  });
});
