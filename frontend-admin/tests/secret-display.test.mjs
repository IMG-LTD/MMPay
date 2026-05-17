import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { maskSecretHandle, providerCredentialFields } from '../src/secretDisplay.js';

describe('provider credential display', () => {
  it('renders only masked secret handles', () => {
    assert.equal(maskSecretHandle('kms://huifu/sandbox/api-key'), 'kms://************-key');
  });

  it('does not expose plaintext credential fields', () => {
    assert.deepEqual(providerCredentialFields(), [
      { label: 'Merchant ID', valueKind: 'secret-handle' },
      { label: 'API Key', valueKind: 'secret-handle' },
      { label: 'Webhook Secret', valueKind: 'secret-handle' },
    ]);
  });
});
