# MMPay Frontend Admin

This app is the MMPay admin frontend rebased on the real soybean-admin project.

## Upstream

- Source: <https://github.com/soybeanjs/soybean-admin>
- Commit: `eba49504280a2866de3a61c65c3401e1453771ce`
- Local record: `UPSTREAM.md`

## MMPay Adaptation

- The home view renders the MMPay dashboard and calls `/api/admin/dashboard`.
- The route is temporarily public because Pig auth is not wired into MMPay yet.
- The validation contract is `scripts/mmpay-soybean-contract.mjs`.

## Commands

```bash
pnpm --dir frontend-admin install --frozen-lockfile
pnpm --dir frontend-admin typecheck
pnpm --dir frontend-admin build
pnpm --dir frontend-admin lint
pnpm --dir frontend-admin test
```
