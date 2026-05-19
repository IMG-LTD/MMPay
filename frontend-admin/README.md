# MMPay Frontend Admin

This app is the MMPay admin console for self-hosted payment operations.
It owns the shipped product routes, locale catalogs, validation contracts,
and operator-facing UI for this repository.

Upstream attribution is recorded only in `UPSTREAM.md`.

## MMPay Adaptation

- The home view renders the MMPay dashboard and calls `/api/admin/dashboard`.
- Merchant and channel views use the upstream admin router, Pinia auth state,
  and Naive UI components for P2 admin operations.
- The validation contract is `scripts/mmpay-frontend-contract.mjs`.

## Commands

```bash
pnpm --dir frontend-admin install --frozen-lockfile
pnpm --dir frontend-admin typecheck
pnpm --dir frontend-admin build
pnpm --dir frontend-admin lint
pnpm --dir frontend-admin test
```
