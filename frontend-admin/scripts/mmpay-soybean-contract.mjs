import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');

async function requireFile(relativePath) {
  await access(path.join(root, relativePath), constants.F_OK);
}

async function readText(relativePath) {
  return readFile(path.join(root, relativePath), 'utf8');
}

async function main() {
  const packageJson = JSON.parse(await readText('package.json'));
  const homeView = await readText('src/views/home/index.vue');
  const routes = await readText('src/router/elegant/routes.ts');
  const appEnv = await readText('.env');

  assert.equal(packageJson.name, 'mmpay-frontend-admin');
  assert.equal(packageJson.dependencies.vue, '3.5.34');
  assert.equal(packageJson.dependencies['naive-ui'], '2.44.1');
  assert.equal(packageJson.dependencies['vue-router'], '5.0.7');
  assert.equal(packageJson.dependencies.echarts, '6.0.0');

  await requireFile('pnpm-workspace.yaml');
  await requireFile('packages/axios/package.json');
  await requireFile('packages/hooks/package.json');
  await requireFile('packages/scripts/package.json');
  await requireFile('src/layouts/base-layout/index.vue');
  await requireFile('src/store/modules/auth/index.ts');
  await requireFile('src/router/elegant/routes.ts');

  assert.match(homeView, /MMPay Admin/);
  assert.match(homeView, /fetch\('\/api\/admin\/dashboard'/);
  assert.match(homeView, /Pig auth pending/);
  assert.match(routes, /constant: true/);
  assert.match(appEnv, /VITE_APP_TITLE=MMPay Admin/);
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
