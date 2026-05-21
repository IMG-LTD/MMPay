/* eslint-disable */
/* prettier-ignore */
// Manually maintained — elegant-router plugin is disabled.

declare module "@elegant-router/types" {
  type ElegantConstRoute = import('@elegant-router/vue').ElegantConstRoute;

  /**
   * route layout
   */
  export type RouteLayout = "base" | "blank";

  /**
   * route map
   */
  export type RouteMap = {
    "root": "/";
    "not-found": "/:pathMatch(.*)*";
    "403": "/403";
    "404": "/404";
    "500": "/500";
    "home": "/home";
    "iframe-page": "/iframe-page/:url";
    "login": "/login";
    "merchants": "/merchants";
    "merchant-detail": "/merchants/:id";
    "merchant-channel-new": "/merchants/:id/channels/new";
    "channel-detail": "/channels/:id";
    "payments": "/payments";
    "payment-detail": "/payments/:id";
    "refunds": "/refunds";
    "refund-detail": "/refunds/:id";
    "refund-new": "/refunds/new";
    "reconciliation": "/reconciliation";
    "reconciliation-detail": "/reconciliation/:id";
    "webhook-out": "/webhook-out";
    "webhook-out-delivery-log-detail": "/webhook-out/delivery-logs/:id";
    "integrations": "/integrations";
    "integration-detail": "/integrations/:id";
    "system": "/system";
  };

  /**
   * route key
   */
  export type RouteKey = keyof RouteMap;

  /**
   * route path
   */
  export type RoutePath = RouteMap[RouteKey];

  /**
   * custom route key
   */
  export type CustomRouteKey = Extract<
    RouteKey,
    | "root"
    | "not-found"
  >;

  /**
   * the generated route key
   */
  export type GeneratedRouteKey = Exclude<RouteKey, CustomRouteKey>;

  /**
   * the first level route key, which contain the layout of the route
   * All routes are single-level (no nested children), so FirstLevelRouteKey = all generated keys.
   */
  export type FirstLevelRouteKey = GeneratedRouteKey;

  /**
   * the last level route key, which has the page component
   * All routes are leaf routes, so LastLevelRouteKey = all generated keys.
   */
  export type LastLevelRouteKey = GeneratedRouteKey;

  /**
   * the single level route key, which has no children
   */
  export type SingleLevelRouteKey = GeneratedRouteKey;

  /**
   * the first level route not single key (none — all routes are single-level)
   */
  export type FirstLevelRouteNotSingleKey = never;

  /**
   * the route key which has children (none)
   */
  export type RouteKeyWithChildren = never;

  /**
   * the child route key
   */
  export type GetChildRouteKey<K extends RouteKey, T extends RouteKey = RouteKey> = T extends `${K}_${infer R}` ? (R extends `${string}_${string}` ? never : T) : never;

  /**
   * the center level route key (none)
   */
  export type CenterLevelRouteKey = never;

  /**
   * the single level route
   */
  type SingleLevelRoute<K extends SingleLevelRouteKey = SingleLevelRouteKey> = K extends string
    ? Omit<ElegantConstRoute, 'component'> & {
        name: K;
        path: RouteMap[K];
        component: `layout.${RouteLayout}$view.${K}`;
        props?: boolean;
        meta: {
          title: string;
          i18nKey?: App.I18n.I18nKey;
          requiresAuth?: boolean;
          keepAlive?: boolean;
          constant?: boolean;
          icon?: string;
          localIcon?: string;
          iconFontSize?: number;
          href?: string;
          hideInMenu?: boolean;
          activeMenu?: import('@elegant-router/types').RouteKey;
          multiTab?: boolean;
          fixedIndexInTab?: number;
          query?: Record<string, string>;
          roles?: import('@elegant-router/types').RouteKey[];
          order?: number;
          homepage?: boolean;
        };
      }
    : never;

  /**
   * the multi level route (none — all routes are single-level)
   */
  type MultiLevelRoute<K extends FirstLevelRouteNotSingleKey = FirstLevelRouteNotSingleKey> = never;

  /**
   * the center level route (none)
   */
  type CenterLevelRoute<K extends CenterLevelRouteKey> = never;

  /**
   * the last level route
   */
  type LastLevelRoute<K extends LastLevelRouteKey> = K extends string
    ? Omit<ElegantConstRoute, 'component'> & {
        name: K;
        path: RouteMap[K];
        component: `view.${K}`;
      }
    : never;

  /**
   * the custom single level route key
   */
  type CustomSingleLevelRouteKey = Extract<CustomRouteKey, SingleLevelRouteKey>;

  /**
   * the custom first level route not single key
   */
  type CustomFirstLevelRouteNotSingleKey = Extract<CustomRouteKey, FirstLevelRouteNotSingleKey>;

  /**
   * the custom center level route key
   */
  type CustomCenterLevelRouteKey = Extract<CustomRouteKey, CenterLevelRouteKey>;

  /**
   * the custom single level route
   */
  type CustomSingleLevelRoute<K extends CustomSingleLevelRouteKey = CustomSingleLevelRouteKey> = K extends string
    ? Omit<ElegantConstRoute, 'component'> & {
        name: K;
        path: RouteMap[K];
        component: `layout.${RouteLayout}$view.${K}`;
      }
    : never;

  /**
   * the custom last level route
   */
  type CustomLastLevelRoute<K extends CustomRouteKey> = K extends string
    ? Omit<ElegantConstRoute, 'component'> & {
        name: K;
        path: RouteMap[K];
        component: `view.${K}`;
      }
    : never;

  /**
   * the custom center level route
   */
  type CustomCenterLevelRoute<K extends CustomRouteKey> = K extends CustomCenterLevelRouteKey
    ? Omit<ElegantConstRoute, 'component'> & {
        name: K;
        path: RouteMap[K];
        children: (CustomCenterLevelRoute<GetChildRouteKey<K>> | CustomLastLevelRoute<GetChildRouteKey<K>>)[];
      }
    : never;

  /**
   * the custom multi level route
   */
  type CustomMultiLevelRoute<K extends CustomFirstLevelRouteNotSingleKey = CustomFirstLevelRouteNotSingleKey> =
    K extends string
      ? ElegantConstRoute & {
          name: K;
          path: RouteMap[K];
          component: `layout.${RouteLayout}`;
          children: (CustomCenterLevelRoute<GetChildRouteKey<K>> | CustomLastLevelRoute<GetChildRouteKey<K>>)[];
        }
      : never;

  /**
   * the custom route
   */
  type CustomRoute = ElegantConstRoute;

  /**
   * the generated route
   */
  type GeneratedRoute = SingleLevelRoute | MultiLevelRoute;

  /**
   * the elegant route
   */
  type ElegantRoute = GeneratedRoute | CustomRoute;
}
