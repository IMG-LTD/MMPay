# MMPay — AI Collaboration Guide

This file governs how AI assistants (Claude Code, Kiro, etc.) work in this repository.
Read it before making any change. Follow every rule here without being asked.

---

## Repository layout

```
backend/          Maven multi-module (Java 21, Spring Boot)
  mmpay-app/      Main application — IAM, payments, merchants, webhooks
  mmpay-admin-api/  Admin dashboard read model
  mmpay-iam/      IAM domain library
  mmpay-gateway-core/  Shared domain types
  mmpay-*/        Other feature modules
frontend-admin/   Vue 3 + TypeScript admin SPA (pnpm, Vite)
.github/workflows/  CI/CD — images.yml and release.yml trigger on v* tags
```

---

## Branch naming

| Purpose | Pattern | Example |
|---|---|---|
| Feature | `feat/<scope>/<short-description>` | `feat/iam/user-management` |
| Bug fix | `fix/<scope>/<short-description>` | `fix/payment/cancel-transaction` |
| Hotfix on release | `hotfix/<version>/<short-description>` | `hotfix/1.0.2/token-race` |
| Chore / deps | `chore/<short-description>` | `chore/bump-spring-boot` |
| Docs | `docs/<short-description>` | `docs/api-reference` |

Rules:
- Use lowercase kebab-case only. No uppercase, no underscores.
- Keep descriptions short (≤ 40 chars after the prefix).
- Never commit directly to `main`. Always open a PR.
- Delete the branch after the PR merges.

---

## Commit message format

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body — wrap at 72 chars]

[optional footer: BREAKING CHANGE, Closes #N]
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `ci`

**Scopes** (use the module or layer name):
`iam`, `payment`, `merchant`, `webhook`, `reconciliation`, `setup`, `audit`,
`frontend`, `frontend-admin`, `ci`, `deps`, `security`, `governance`

Examples:
```
feat(iam): add user management CRUD endpoints
fix(security): consume setup token before creating admin
chore(deps): bump spring-boot to 3.4.1
docs(release): record v1.0.2 image digests
```

Rules:
- Summary line ≤ 72 chars, imperative mood, no trailing period.
- One logical change per commit. Do not bundle unrelated fixes.
- Never use `--amend` on commits that have been pushed.
- Never use `--no-verify` to skip hooks.

---

## Version and release conventions

This project uses **SemVer** (`MAJOR.MINOR.PATCH`):

| Increment | When |
|---|---|
| PATCH | Bug fixes, security patches, dependency bumps with no API change |
| MINOR | New features, new endpoints, new UI pages — backwards compatible |
| MAJOR | Breaking API changes, schema migrations requiring downtime |

**Tag format:** `v<MAJOR>.<MINOR>.<PATCH>` — e.g. `v1.0.3`

**Hotfix tags:** `v<MAJOR>.<MINOR>.<PATCH>-hotfix.<N>` — e.g. `v1.0.2-hotfix.1`

**Tag creation is restricted by GitHub repo governance rules.**
Tags must be created via the GitHub UI or via `workflow_dispatch` on `images.yml` / `release.yml`.
Never attempt `git push origin v*` — it will be rejected.

**Release checklist (for humans):**
1. Merge all intended PRs to `main`.
2. Create the tag via GitHub UI → Releases → "Draft a new release".
3. The `images.yml` workflow builds and pushes Docker images automatically.
4. The `release.yml` workflow publishes the GitHub Release with changelog.
5. Record the image digests in a `docs/` commit after the workflow completes.

---

## Code conventions

### Backend (Java / Spring Boot)

- Java 21. Use records for DTOs and value objects.
- All mutating service methods must be `@Transactional`.
- All controller endpoints must have `@PreAuthorize`. No unauthenticated endpoints except `/setup`, `/login`, and actuator health.
- Role constants: stored lowercase in DB (`admin`, `ops`, `finance`, `auditor`), returned uppercase from API (`ADMIN`, `OPS`, `FINANCE`, `AUDITOR`). `@PreAuthorize` uses uppercase: `hasRole('ADMIN')`.
- Never make a service class `final` if it has `@Transactional` methods — CGLIB cannot proxy final classes.
- Flyway migrations live in `src/main/resources/db/migration/`. Use `V<N>__<description>.sql`. Never edit an existing migration; always add a new one.
- Test schema lives in `src/test/resources/test-iam-schema.sql`. Keep it in sync with production migrations.
- Validate at system boundaries only (controller layer or service entry points). Trust internal invariants.
- No comments unless the WHY is non-obvious.

### Frontend (Vue 3 / TypeScript)

- `VITE_AUTH_ROUTE_MODE=static` — routes are defined in `src/router/elegant/routes.ts`, not fetched from backend.
- `VITE_STATIC_SUPER_ROLE=ADMIN` — ADMIN bypasses all `meta.roles` checks via `isStaticSuper`.
- All non-constant routes must declare `meta.roles` with the allowed uppercase role array.
- Role-to-route mapping: `['ADMIN']` for system/integrations; `['ADMIN','OPS']` for merchants/webhooks; `['ADMIN','OPS','FINANCE']` for payments/refunds/reconciliation.
- API calls go through `src/service/api/`. Never call `fetch` or `axios` directly in views.
- Use `pnpm` — never `npm` or `yarn` in this project.

---

## Testing

- Run backend tests: `cd backend && mvn test`
- Run frontend build check: `cd frontend-admin && npm run build`
- All tests must pass before pushing. Never push with failing tests.
- Do not mock the database in integration tests — use the H2 in-memory schema.
- After any schema change, update `test-iam-schema.sql` to match.

---

## Security rules (non-negotiable)

- Never log passwords, tokens, or secrets — not even hashed values.
- Never commit `.env` files, private keys, or credential files.
- Setup endpoint (`/setup`) is one-shot: token is consumed atomically before admin creation.
- Self-delete and self-role-change are blocked at the service layer, not just the UI.
- `@PreAuthorize` is the authoritative access control. Frontend role checks are UX only.

---

## What AI must NOT do without explicit user confirmation

- Push to `main` directly.
- Create or push version tags (`v*`).
- Delete remote branches (except stale ones the user explicitly lists).
- Run `git push --force` or `git reset --hard` on pushed commits.
- Modify `.github/workflows/` files.
- Drop or truncate database tables.
- Amend commits that have already been pushed.

---

## Stale branch cleanup

Before deleting any remote branch, confirm with the user. Dependabot branches
(`dependabot/*`) are managed automatically — do not delete them manually.
Merged feature/fix branches should be deleted after PR merge.
