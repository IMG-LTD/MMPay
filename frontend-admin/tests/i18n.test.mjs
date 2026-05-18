import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { adminMessages, credentialFieldLabels, t } from '../src/i18n.js';

describe('admin i18n messages', () => {
  it('covers zh-CN and en-US admin shell labels', () => {
    assert.equal(t('en-US', 'admin.title'), 'MMPay Admin');
    assert.equal(t('zh-CN', 'admin.title'), 'MMPay 管理后台');
    assert.equal(t('en-US', 'credential.apiKey'), 'API Key');
    assert.equal(t('zh-CN', 'credential.apiKey'), 'API 密钥');
    assert.equal(t('en-US', 'nav.invoices'), 'Invoices');
    assert.equal(t('zh-CN', 'nav.invoices'), '发票');
  });

  it('renders provider credential labels through locale keys', () => {
    assert.deepEqual(credentialFieldLabels('en-US'), ['Merchant ID', 'API Key', 'Webhook Secret']);
    assert.deepEqual(credentialFieldLabels('zh-CN'), ['商户号', 'API 密钥', 'Webhook 密钥']);
    assert.deepEqual(Object.keys(adminMessages).sort(), ['en-US', 'zh-CN']);
  });
});
