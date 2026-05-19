import { strict as assert } from 'node:assert';
import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { describe, it } from 'node:test';

const root = process.cwd();

async function read(path) {
  return readFile(join(root, path), 'utf8');
}

function exists(path) {
  return existsSync(join(root, path));
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

  it('blocks GA unless remote v1 tag protection evidence is verified', async () => {
    const releaseGate = await read('scripts/release-gate.sh');
    const validateLocal = await read('scripts/validate-local.sh');

    assert.equal(exists('docs/release/v1-tag-ruleset-evidence-template.md'), true);
    assert.equal(exists('scripts/governance/verify-v1-tag-ruleset-evidence.sh'), true);
    assert.match(releaseGate, /v1\.0\.0-v1-tag-ruleset-evidence\.md/);
    assert.match(releaseGate, /verify-v1-tag-ruleset-evidence\.sh/);
    assert.match(validateLocal, /verify-v1-tag-ruleset-evidence\.sh/);
  });
});
