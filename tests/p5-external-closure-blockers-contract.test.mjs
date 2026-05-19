import { strict as assert } from 'node:assert';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const root = process.cwd();

async function read(path) {
  return readFile(join(root, path), 'utf8');
}

describe('P5 external closure blockers contract', () => {
  it('documents every external artifact that blocks v1.0.0 GA', async () => {
    const blockers = await read('docs/release/external-closure-blockers.md');

    for (const required of [
      'docs/release/v1.0.0-image-digest-evidence.md',
      'docs/release/v1.0.0-e2e-evidence.md',
      'docs/release/backup-restore-drill-evidence.md',
      'docs/release/vendor-binding/v1.0.0-BINDING_OK.asc',
      'governance/github-rulesets/v1-tags.json',
      'governance/mmpay-binding.yaml'
    ]) {
      assert.match(blockers, new RegExp(required.replaceAll('.', '\\.')));
    }

    for (const status of [
      'blocked-external',
      'release-gate-blocking',
      'upstream-flip-blocking'
    ]) {
      assert.match(blockers, new RegExp(status));
    }
  });

  it('keeps the blocker register wired into local validation', async () => {
    const validateLocal = await read('scripts/validate-local.sh');

    assert.match(validateLocal, /p5-external-closure-blockers-contract\.test\.mjs/);
  });
});
