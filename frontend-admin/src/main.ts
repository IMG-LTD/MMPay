import { createApp, h } from 'vue';
import { createPinia } from 'pinia';
import { NCard, NConfigProvider, NDataTable, NMessageProvider, NTag } from 'naive-ui';
import { renderProviderCredentialRows } from './app.js';

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
      const rows = renderProviderCredentialRows();
      const columns = [
        { title: 'Provider field', key: 'label' },
        {
          title: 'Value kind',
          key: 'valueKind',
          render: (row: { valueKind: string }) => h(NTag, { type: 'info' }, () => row.valueKind),
        },
        { title: 'Secret handle', key: 'displayValue' },
      ];

      return () =>
        h(NConfigProvider, null, {
          default: () =>
            h(NMessageProvider, null, {
              default: () =>
                h(NCard, { title: 'MMPay Admin' }, () => [
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
