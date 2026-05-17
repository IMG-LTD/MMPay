import { maskSecretHandle, providerCredentialFields } from './secretDisplay.js';

const sampleHandles = {
  'Merchant ID': 'kms://huifu/sandbox/merchant-id',
  'API Key': 'kms://huifu/sandbox/api-key',
  'Webhook Secret': 'kms://mmpay/mmmail/webhook-secret',
};

/**
 * @returns {{ label: string, valueKind: string, displayValue: string }[]}
 */
export function renderProviderCredentialRows() {
  return providerCredentialFields().map((field) => ({
    label: field.label,
    valueKind: field.valueKind,
    displayValue: maskSecretHandle(sampleHandles[field.label]),
  }));
}
