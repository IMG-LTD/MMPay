import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, readFile, writeFile } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import os from 'node:os';
import path from 'node:path';

const execFileAsync = promisify(execFile);
const root = path.resolve(import.meta.dirname, '..');
const validator = path.join(root, 'scripts/validate-e2e-evidence.sh');
const renderer = path.join(root, 'scripts/render-e2e-evidence.sh');
const template = path.join(root, 'docs/release/e2e-evidence-template.md');

describe('MP-8 redacted evidence contract', () => {
  it('documents all required MMMail and MMPay evidence fields', async () => {
    const templateText = await readFile(template, 'utf8');

    assert.match(templateText, /Evidence status: completed-external-evidence/);
    assert.match(templateText, /MMPay repository commit SHA:/);
    assert.match(templateText, /MMMail public release commit SHA:/);
    assert.match(templateText, /Provider event ID:/);
    assert.match(templateText, /MMMail webhook event ID:/);
    assert.match(templateText, /License claim ID:/);
    assert.match(templateText, /Run finished at:/);
    assert.match(templateText, /happy:/);
    assert.match(templateText, /bad-signature:/);
    assert.match(templateText, /expired-window:/);
    assert.match(templateText, /provider-error:/);
    assert.match(templateText, /replay:/);
  });

  it('accepts a redacted sandbox-with-real-money evidence file with real identifiers', async () => {
    const evidenceFile = await writeEvidenceFile(validEvidence());

    const result = await execFileAsync('bash', [validator, evidenceFile]);

    assert.match(result.stdout, /mmpay e2e evidence markers are complete/);
  });

  it('rejects placeholders, mock provider mode, and missing failure scenarios', async () => {
    const evidenceFile = await writeEvidenceFile(invalidMockEvidence());

    await assert.rejects(
      execFileAsync('bash', [validator, evidenceFile]),
      /Provider environment must be sandbox-with-real-money or live|Missing required marker: replay:/,
    );
  });

  it('renders validated evidence only from explicit external run facts', async () => {
    const result = await execFileAsync('bash', [renderer], {
      cwd: root,
      env: {
        ...process.env,
        ...validRenderEnv(),
      },
    });
    const evidenceFile = await writeEvidenceFile(result.stdout);

    const validation = await execFileAsync('bash', [validator, evidenceFile]);

    assert.match(validation.stdout, /mmpay e2e evidence markers are complete/);
    assert.match(result.stdout, /Provider event ID: hf_evt_redacted_001/);
  });

  it('does not render evidence when required external facts are missing', async () => {
    await assert.rejects(execFileAsync('bash', [renderer], { cwd: root }), /Missing required evidence variable:/);
  });
});

async function writeEvidenceFile(content) {
  const dir = await mkdtemp(path.join(os.tmpdir(), 'mmpay-evidence-'));
  const evidenceFile = path.join(dir, 'e2e-evidence.md');
  await writeFile(evidenceFile, content);
  return evidenceFile;
}

function validEvidence() {
  return `# MMPay MMMail E2E Evidence

Evidence status: completed-external-evidence
Payment provider: huifu
Provider environment: sandbox-with-real-money
MMPay repository commit SHA: 0123456789abcdef0123456789abcdef01234567
MMMail public release commit SHA: 89abcdef0123456789abcdef0123456789abcdef
Provider event ID: hf_evt_redacted_001
MMMail webhook event ID: wh_evt_redacted_001
License claim ID: claim_redacted_001
Run finished at: 2026-05-17T13:00:00Z

happy: provider event hf_evt_redacted_001 delivered to MMMail webhook wh_evt_redacted_001
bad-signature: event evt_bad_sig_redacted rejected with explicit signature failure
expired-window: event evt_expired_redacted rejected with explicit timestamp failure
provider-error: event evt_provider_error_redacted preserved unpaid state
replay: event evt_replay_redacted duplicate rejected
`;
}

function invalidMockEvidence() {
  return validEvidence()
    .replace('Provider environment: sandbox-with-real-money', 'Provider environment: mock')
    .replace('replay: event evt_replay_redacted duplicate rejected', '');
}

function validRenderEnv() {
  return {
    MMPAY_EVIDENCE_PROVIDER: 'huifu',
    MMPAY_EVIDENCE_ENVIRONMENT: 'sandbox-with-real-money',
    MMPAY_EVIDENCE_MMMAIL_SHA: '89abcdef0123456789abcdef0123456789abcdef',
    MMPAY_EVIDENCE_PROVIDER_EVENT_ID: 'hf_evt_redacted_001',
    MMPAY_EVIDENCE_MMMAIL_WEBHOOK_EVENT_ID: 'wh_evt_redacted_001',
    MMPAY_EVIDENCE_LICENSE_CLAIM_ID: 'claim_redacted_001',
    MMPAY_EVIDENCE_RUN_FINISHED_AT: '2026-05-17T13:00:00Z',
    MMPAY_EVIDENCE_HAPPY: 'provider event hf_evt_redacted_001 delivered to MMMail webhook wh_evt_redacted_001',
    MMPAY_EVIDENCE_BAD_SIGNATURE: 'event evt_bad_sig_redacted rejected with explicit signature failure',
    MMPAY_EVIDENCE_EXPIRED_WINDOW: 'event evt_expired_redacted rejected with explicit timestamp failure',
    MMPAY_EVIDENCE_PROVIDER_ERROR: 'event evt_provider_error_redacted preserved unpaid state',
    MMPAY_EVIDENCE_REPLAY: 'event evt_replay_redacted duplicate rejected',
  };
}
