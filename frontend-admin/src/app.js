import { maskSecretHandle, providerCredentialFields } from './secretDisplay.js';

const sampleHandles = {
  'Merchant ID': 'kms://huifu/sandbox/merchant-id',
  'API Key': 'kms://huifu/sandbox/api-key',
  'Webhook Secret': 'kms://mmpay/mmmail/webhook-secret',
};

/**
 * @param {'en-US' | 'zh-CN'} [locale]
 * @returns {{ label: string, valueKind: string, displayValue: string }[]}
 */
export function renderProviderCredentialRows(locale = 'en-US') {
  return providerCredentialFields(locale).map((field, index) => ({
    label: field.label,
    valueKind: field.valueKind,
    displayValue: maskSecretHandle(Object.values(sampleHandles)[index]),
  }));
}
