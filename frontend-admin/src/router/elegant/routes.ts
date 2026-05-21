/* eslint-disable */
/* prettier-ignore */
// Manually maintained — elegant-router plugin is disabled.

import type { GeneratedRoute } from '@elegant-router/types';

export const generatedRoutes: GeneratedRoute[] = [
  {
    name: '403',
    path: '/403',
    component: 'layout.blank$view.403',
    meta: {
      title: '403',
      i18nKey: 'route.403',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: '404',
    path: '/404',
    component: 'layout.blank$view.404',
    meta: {
      title: '404',
      i18nKey: 'route.404',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: '500',
    path: '/500',
    component: 'layout.blank$view.500',
    meta: {
      title: '500',
      i18nKey: 'route.500',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: 'home',
    path: '/home',
    component: 'layout.base$view.home',
    meta: {
      title: 'home',
      i18nKey: 'route.home',
      icon: 'mdi:monitor-dashboard',
      order: 1
    }
  },
  {
    name: 'iframe-page',
    path: '/iframe-page/:url',
    component: 'layout.base$view.iframe-page',
    props: true,
    meta: {
      title: 'iframe-page',
      i18nKey: 'route.iframe-page',
      constant: true,
      hideInMenu: true,
      keepAlive: true
    }
  },
  {
    name: 'login',
    path: '/login',
    component: 'layout.blank$view.login',
    meta: {
      title: 'login',
      i18nKey: 'route.login',
      constant: true,
      hideInMenu: true
    }
  },
  {
    name: 'merchants',
    path: '/merchants',
    component: 'layout.base$view.merchants',
    meta: {
      title: 'merchants',
      i18nKey: 'route.merchants',
      icon: 'mdi:store-cog',
      order: 2,
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'merchant-detail',
    path: '/merchants/:id',
    component: 'layout.base$view.merchant-detail',
    props: true,
    meta: {
      title: 'merchant-detail',
      i18nKey: 'route.merchant-detail',
      hideInMenu: true,
      activeMenu: 'merchants',
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'merchant-channel-new',
    path: '/merchants/:id/channels/new',
    component: 'layout.base$view.merchant-channel-new',
    props: true,
    meta: {
      title: 'merchant-channel-new',
      i18nKey: 'route.merchant-channel-new',
      hideInMenu: true,
      activeMenu: 'merchants',
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'channel-detail',
    path: '/channels/:id',
    component: 'layout.base$view.channel-detail',
    props: true,
    meta: {
      title: 'channel-detail',
      i18nKey: 'route.channel-detail',
      hideInMenu: true,
      activeMenu: 'merchants',
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'payments',
    path: '/payments',
    component: 'layout.base$view.payments',
    meta: {
      title: 'payments',
      i18nKey: 'route.payments',
      icon: 'mdi:credit-card-outline',
      order: 3,
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'payment-detail',
    path: '/payments/:id',
    component: 'layout.base$view.payment-detail',
    props: true,
    meta: {
      title: 'payment-detail',
      i18nKey: 'route.payment-detail',
      hideInMenu: true,
      activeMenu: 'payments',
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'refunds',
    path: '/refunds',
    component: 'layout.base$view.refunds',
    meta: {
      title: 'refunds',
      i18nKey: 'route.refunds',
      icon: 'mdi:cash-refund',
      order: 4,
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'refund-detail',
    path: '/refunds/:id',
    component: 'layout.base$view.refund-detail',
    props: true,
    meta: {
      title: 'refund-detail',
      i18nKey: 'route.refund-detail',
      hideInMenu: true,
      activeMenu: 'refunds',
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'refund-new',
    path: '/refunds/new',
    component: 'layout.base$view.refund-new',
    meta: {
      title: 'refund-new',
      i18nKey: 'route.refund-new',
      hideInMenu: true,
      activeMenu: 'refunds',
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'reconciliation',
    path: '/reconciliation',
    component: 'layout.base$view.reconciliation',
    meta: {
      title: 'reconciliation',
      i18nKey: 'route.reconciliation',
      icon: 'mdi:clipboard-check-outline',
      order: 5,
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'reconciliation-detail',
    path: '/reconciliation/:id',
    component: 'layout.base$view.reconciliation-detail',
    props: true,
    meta: {
      title: 'reconciliation-detail',
      i18nKey: 'route.reconciliation-detail',
      hideInMenu: true,
      activeMenu: 'reconciliation',
      roles: ['ADMIN', 'OPS', 'FINANCE']
    }
  },
  {
    name: 'webhook-out',
    path: '/webhook-out',
    component: 'layout.base$view.webhook-out',
    meta: {
      title: 'webhook-out',
      i18nKey: 'route.webhook-out',
      icon: 'mdi:webhook',
      order: 6,
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'webhook-out-delivery-log-detail',
    path: '/webhook-out/delivery-logs/:id',
    component: 'layout.base$view.webhook-out-delivery-log-detail',
    props: true,
    meta: {
      title: 'webhook-out-delivery-log-detail',
      i18nKey: 'route.webhook-out-delivery-log-detail',
      hideInMenu: true,
      activeMenu: 'webhook-out',
      roles: ['ADMIN', 'OPS']
    }
  },
  {
    name: 'integrations',
    path: '/integrations',
    component: 'layout.base$view.integrations',
    meta: {
      title: 'integrations',
      i18nKey: 'route.integrations',
      icon: 'mdi:connection',
      order: 7,
      roles: ['ADMIN']
    }
  },
  {
    name: 'integration-detail',
    path: '/integrations/:id',
    component: 'layout.base$view.integration-detail',
    props: true,
    meta: {
      title: 'integration-detail',
      i18nKey: 'route.integration-detail',
      hideInMenu: true,
      activeMenu: 'integrations',
      roles: ['ADMIN']
    }
  },
  {
    name: 'system',
    path: '/system',
    component: 'layout.base$view.system',
    meta: {
      title: 'system',
      i18nKey: 'route.system',
      icon: 'mdi:cog-outline',
      order: 8,
      roles: ['ADMIN']
    }
  }
];
