import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { NCard, NConfigProvider, NDataTable, NMessageProvider, NTag } from 'naive-ui';
import { renderProviderCredentialRows } from './app.js';
import { t } from './i18n.js';

export const soybeanAdminStack = Object.freeze({
  upstream: 'soybean-admin',
  ui: 'naive-ui',
  state: 'pinia',
  build: 'vite',
});

export function createMmpayAdminApp() {
  return createApp({
    name: 'MmpaySoybeanAdminShell',
    setup() {
      const locale = 'en-US';
      const rows = renderProviderCredentialRows(locale);
      const columns = [
        { title: t(locale, 'table.providerField'), key: 'label' },
        {
          title: t(locale, 'table.valueKind'),
          key: 'valueKind',
          render: (row: { valueKind: string }) => h(NTag, { type: 'info' }, () => row.valueKind),
        },
        { title: t(locale, 'table.secretHandle'), key: 'displayValue' },
      ];

      return () =>
        h(NConfigProvider, null, {
          default: () =>
            h(NMessageProvider, null, {
              default: () =>
                h(NCard, { title: t(locale, 'admin.title') }, () => [
                  h(NDataTable, { columns, data: rows, pagination: false, singleLine: false }),
                ]),
            }),
        });
    },
  }).use(createPinia());
}

const mountPoint = document.querySelector('#app');
if (mountPoint) {
  createMmpayAdminApp().mount(mountPoint);
}
