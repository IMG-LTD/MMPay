import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { renderAdminDashboard } from '../src/app.js';

const adminRoot = path.resolve(import.meta.dirname, '..');

describe('admin surface coverage', () => {
  it('renders the operational views required by MP-5', () => {
    const dashboard = renderAdminDashboard('en-US');

    assert.deepEqual(
      dashboard.navigation.map((item) => item.key),
      ['merchants', 'channels', 'orders', 'refunds', 'invoices', 'webhook-logs', 'reconciliation'],
    );
    assert.equal(dashboard.metricCards.length, 4);
    assert.deepEqual(dashboard.metricCards.map((metric) => metric.value), ['0', '0', '0', '0']);
    assert.equal(dashboard.tables.length, 7);
    assert.deepEqual(
      dashboard.tables.map((table) => table.key),
      ['credentials', 'channels', 'orders', 'refunds', 'invoices', 'webhook-logs', 'reconciliation'],
    );
  });

  it('does not seed fake payment success rows before a live provider is connected', () => {
    const dashboard = renderAdminDashboard('en-US');
    const runtimeTables = dashboard.tables.filter((table) => table.key !== 'credentials' && table.key !== 'channels');
    const channels = dashboard.tables.find((table) => table.key === 'channels');
    const invoices = dashboard.tables.find((table) => table.key === 'invoices');

    assert.ok(channels);
    assert.ok(invoices);
    assert.equal(channels.rows[0].status, 'credentials-required');
    assert.equal(invoices.rows[0].reason, 'unsupported by current provider');
    assert.deepEqual(runtimeTables.map((table) => table.rows.length), [0, 0, 1, 0, 0]);
    assert.ok(runtimeTables.every((table) => !table.rows.some((row) => row.status === 'succeeded')));
  });

  it('keeps credential rows handle-only without plaintext secret fields', () => {
    const dashboard = renderAdminDashboard('en-US');
    const credentials = dashboard.tables.find((table) => table.key === 'credentials');

    assert.ok(credentials);
    assert.equal(credentials.rows.length, 3);
    for (const row of credentials.rows) {
      assert.equal(row.valueKind, 'secret-handle');
      assert.match(row.displayValue, /^kms:\/\/\*+/);
      assert.doesNotMatch(row.displayValue, /sandbox\/api-key|webhook-secret|merchant-id/);
    }
  });

  it('uses Naive UI layout components for the shell', async () => {
    const mainTs = await readFile(path.join(adminRoot, 'src/main.ts'), 'utf8');

    assert.match(mainTs, /NLayout/);
    assert.match(mainTs, /NLayoutSider/);
    assert.match(mainTs, /NMenu/);
    assert.match(mainTs, /NDataTable/);
    assert.doesNotMatch(mainTs, /document\.createElement\('button'\)/);
  });
});
