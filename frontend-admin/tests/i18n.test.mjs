import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { adminMessages, credentialFieldLabels, t } from '../src/i18n.js';

describe('admin i18n messages', () => {
  it('covers zh-CN and en-US admin shell labels', () => {
    assert.equal(t('en-US', 'admin.title'), 'MMPay Admin');
    assert.equal(t('zh-CN', 'admin.title'), 'MMPay 管理后台');
    assert.equal(t('en-US', 'credential.rsaPrivateKey'), 'RSA Private Key');
    assert.equal(t('zh-CN', 'credential.rsaPrivateKey'), 'RSA 私钥');
    assert.equal(t('en-US', 'nav.invoices'), 'Invoices');
    assert.equal(t('zh-CN', 'nav.invoices'), '发票');
  });

  it('renders provider credential labels through locale keys', () => {
    assert.deepEqual(credentialFieldLabels('en-US'), [
      'Sys ID',
      'Product ID',
      'RSA Public Key',
      'RSA Private Key',
      'Merchant ID',
      'Notify URL',
      'Webhook Endpoint Key',
      'Skill Source',
    ]);
    assert.deepEqual(credentialFieldLabels('zh-CN'), [
      '系统号',
      '产品号',
      'RSA 公钥',
      'RSA 私钥',
      '商户号',
      '通知地址',
      'Webhook 终端密钥',
      'Skill Source',
    ]);
    assert.deepEqual(Object.keys(adminMessages).sort(), ['en-US', 'zh-CN']);
  });
});
