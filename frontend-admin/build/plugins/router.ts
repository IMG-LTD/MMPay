import type { RouteMeta } from 'vue-router';
import ElegantVueRouter from '@elegant-router/vue/vite';
import type { RouteKey } from '@elegant-router/types';

export function setupElegantRouter() {
  return ElegantVueRouter({
    layouts: {
      base: 'src/layouts/base-layout/index.vue',
      blank: 'src/layouts/blank-layout/index.vue'
    },
    // Only scan _builtin views; all app routes are declared in customRoutes below.
    pageExcludePatterns: ['**/!(index).vue', '**/components/**', '!**/_builtin/**'],
    customRoutes: {
      map: {
        root: '/',
        'not-found': '/:pathMatch(.*)*',
        home: '/home',
        merchants: '/merchants',
        'merchant-detail': '/merchants/:id',
        'merchant-channel-new': '/merchants/:id/channels/new',
        'channel-detail': '/channels/:id',
        payments: '/payments',
        'payment-detail': '/payments/:id',
        refunds: '/refunds',
        'refund-detail': '/refunds/:id',
        'refund-new': '/refunds/new',
        reconciliation: '/reconciliation',
        'reconciliation-detail': '/reconciliation/:id',
        'webhook-out': '/webhook-out',
        'webhook-out-delivery-log-detail': '/webhook-out/delivery-logs/:id',
        integrations: '/integrations',
        'integration-detail': '/integrations/:id'
      }
    },
    routePathTransformer(routeName, routePath) {
      return routePath;
    },
    onRouteMetaGen(routeName) {
      const key = routeName as RouteKey;

      const constantRoutes: RouteKey[] = ['login', '403', '404', '500'];

      const meta: Partial<RouteMeta> = {
        title: key,
        i18nKey: `route.${key}` as App.I18n.I18nKey
      };

      if (constantRoutes.includes(key)) {
        meta.constant = true;
      }

      return meta;
    }
  });
}
