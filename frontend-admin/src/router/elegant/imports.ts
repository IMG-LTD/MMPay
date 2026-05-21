/* eslint-disable */
/* prettier-ignore */
// Manually maintained — elegant-router plugin is disabled.

import type { RouteComponent } from "vue-router";
import type { LastLevelRouteKey, RouteLayout } from "@elegant-router/types";

import BaseLayout from "@/layouts/base-layout/index.vue";
import BlankLayout from "@/layouts/blank-layout/index.vue";

export const layouts: Record<RouteLayout, RouteComponent | (() => Promise<RouteComponent>)> = {
  base: BaseLayout,
  blank: BlankLayout,
};

export const views: Record<LastLevelRouteKey, RouteComponent | (() => Promise<RouteComponent>)> = {
  403: () => import("@/views/_builtin/403/index.vue"),
  404: () => import("@/views/_builtin/404/index.vue"),
  500: () => import("@/views/_builtin/500/index.vue"),
  "iframe-page": () => import("@/views/_builtin/iframe-page/[url].vue"),
  login: () => import("@/views/_builtin/login/index.vue"),
  home: () => import("@/views/home/index.vue"),
  merchants: () => import("@/views/merchants/index.vue"),
  "merchant-detail": () => import("@/views/merchants/detail/index.vue"),
  "merchant-channel-new": () => import("@/views/merchants/channel-new/index.vue"),
  "channel-detail": () => import("@/views/channels/detail/index.vue"),
  payments: () => import("@/views/payments/index.vue"),
  "payment-detail": () => import("@/views/payments/detail/index.vue"),
  refunds: () => import("@/views/refunds/index.vue"),
  "refund-detail": () => import("@/views/refunds/detail/index.vue"),
  "refund-new": () => import("@/views/refunds/new/index.vue"),
  reconciliation: () => import("@/views/reconciliation/index.vue"),
  "reconciliation-detail": () => import("@/views/reconciliation/detail/index.vue"),
  "webhook-out": () => import("@/views/webhook-out/index.vue"),
  "webhook-out-delivery-log-detail": () => import("@/views/webhook-out/delivery-log-detail/index.vue"),
  integrations: () => import("@/views/integrations/index.vue"),
  "integration-detail": () => import("@/views/integrations/detail/index.vue"),
  system: () => import("@/views/system/index.vue"),
};
