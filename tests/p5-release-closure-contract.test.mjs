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

describe('P5 release closure contract', () => {
  it('ships rc cadence, GA promotion, and immutable v1 tag governance', async () => {
    const releaseGate = await read('scripts/release-gate.sh');
    const rcCadence = await read('docs/release/rc-cadence.md');
    const ruleset = await read('governance/github-rulesets/v1-tags.json');
    const install = await read('docs/ops/install.md');

    assert.match(releaseGate, /--rc/);
    assert.match(releaseGate, /--ga/);
    assert.match(releaseGate, /evidence-refresh-no-op/);
    assert.match(rcCadence, /fresh-rc per change/i);
    assert.match(rcCadence, /v1\.0\.0-rc\.N/);
    assert.match(ruleset, /refs\/tags\/v1\.\*/);
    assert.match(ruleset, /deletion/);
    assert.match(ruleset, /non_fast_forward/);
    assert.match(install, /receive\.denyDeletes=true/);
    assert.match(install, /receive\.denyNonFastForwards=true/);
  });

  it('ships image digest, vendor binding, and evidence validation surfaces', async () => {
    const template = await read('docs/release/image-digest-evidence-template.md');
    const verifier = await read('scripts/governance/verify-image-digest-evidence.sh');
    const releaseGate = await read('scripts/release-gate.sh');

    for (const file of [
      'governance/cosign-keys.yaml',
      'governance/registry-tls-pins.yaml',
      'governance/vendor-keys.yaml',
      'governance/operator-keys.yaml',
      'governance/shamir-policy.md',
      'governance/restore-nonces/.gitkeep',
      'docs/release/vendor-binding/.gitkeep'
    ]) {
      assert.equal(exists(file), true, `${file} is required by P5`);
    }

    assert.match(template, /mmpay-app/);
    assert.match(template, /compose_digest/);
    assert.match(template, /build_attestation/);
    assert.match(verifier, /registry-tls-pins\.yaml/);
    assert.match(verifier, /cosign-keys\.yaml/);
    assert.match(verifier, /registry_immutability_proof/);
    assert.match(releaseGate, /v1\.0\.0-BINDING_OK\.asc/);
    assert.match(releaseGate, /Vendor PGP/);
  });

  it('ships P5 operator runbooks and upstream flip contract', async () => {
    const requiredDocs = [
      'docs/ops/backup-restore-drill.md',
      'docs/ops/bootstrap-compromise-rotation.md',
      'docs/ops/degraded-startup.md',
      'docs/ops/runbook.md',
      'docs/governance/deprecation-policy.md',
      'docs/integrations/upstream-evidence-flip-contract.md',
      'docs/release/v1.0.0-release-notes.md'
    ];
    for (const file of requiredDocs) {
      assert.equal(exists(file), true, `${file} is required by P5`);
    }

    const degraded = await read('docs/ops/degraded-startup.md');
    const drill = await read('docs/ops/backup-restore-drill.md');
    const deprecation = await read('docs/governance/deprecation-policy.md');
    const flip = await read('docs/integrations/upstream-evidence-flip-contract.md');

    assert.match(degraded, /MMPAY_FAIL_OPEN_DEGRADED_UI=true/);
    assert.match(degraded, /api\/admin\/audit\/verify/);
    assert.match(degraded, /api\/license-relay\/v1\/forward/);
    assert.match(drill, /MMPAY_AUDIT_RESTORE_ATTESTATION_KEY/);
    assert.match(drill, /MMPAY_EVIDENCE_SIGNING_KEY/);
    assert.match(deprecation, /sole live adapter/i);
    assert.match(flip, /v1\.0\.0-image-digest-evidence\.md/);
    assert.match(flip, /governance\/mmpay-binding\.yaml/);
  });

  it('wires P5 tests and governance checks into local validation', async () => {
    const validateLocal = await read('scripts/validate-local.sh');
    const releaseGate = await read('scripts/release-gate.sh');

    assert.match(validateLocal, /p5-release-closure-contract\.test\.mjs/);
    assert.match(validateLocal, /P5ReleaseClosureContractTest/);
    assert.match(validateLocal, /AuditSegmentVerifierTest/);
    assert.match(validateLocal, /AuditVerifySegmentRestartContractTest/);
    assert.match(validateLocal, /verify-image-digest-evidence\.sh/);
    assert.match(releaseGate, /verify-image-digest-evidence\.sh/);
    assert.equal(exists('scripts/governance/audit-chain-verify-cli.sh'), true);
  });
});
