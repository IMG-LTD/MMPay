# MMPay Frontend Upstream

MMPay `frontend-admin` is rebased on the real soybean-admin project instead of a hand-written static page.

| Item | Value |
| --- | --- |
| Upstream | <https://github.com/soybeanjs/soybean-admin> |
| Branch | `main` |
| Commit | `eba49504280a2866de3a61c65c3401e1453771ce` |
| Imported scope | Vue app, Soybean layout system, router, stores, build packages, UnoCSS, Naive UI integration |
| MMPay adaptation | `src/views/home/index.vue`, branding, route metadata, validation contract |

The current MMPay page calls `/api/admin/dashboard` directly. It does not fake login state; the home route is marked as a constant public operations page until Pig auth is wired into MMPay.
