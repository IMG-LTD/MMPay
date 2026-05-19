# Backend IAM / Pig Alignment

| Item | Value |
| --- | --- |
| IAM stack | Spring Authorization Server (SAS) on Spring Boot 3 |
| Reference inspiration | <https://gitee.com/log4j/pig> commit `28ef625701ebe047984661a61589330b9360d43e` |
| Vendoring | **None.** No pig source is forked or copied into this repo |
| pig modules tracked for *concept* parity | auth, upms (RBAC), common |
| Current runtime status | Direct SAS + custom upms; pig is reference-only |

## Decision (§15 alignment with P1 spec debate resolution)

The P1 spec v1.0 draft proposed a "pig single-jar trim" that forked
`pig-auth` and `pig-upms` into a new `mmpay-iam` Maven module with
`pig-common-*` dependencies rewritten. The implementation took a
different path: **MMPay authenticates directly on Spring Authorization
Server tables (`oauth2_*`), backed by a custom `JdbcIamRepository` and
`OpaqueTokenService`**. No pig source is vendored.

Reasons the implementation diverged from the literal spec text:

- `pig-common-*` pulls Nacos discovery / Sentinel / Spring Cloud
  Gateway transitively; the trim-and-rewrite work to remove every
  Spring Cloud Alibaba transitive dep was disproportionate to the
  payment-gateway use case (single-jar, single-tenant).
- The pig RBAC schema (`sys_user`, `sys_role`, `sys_user_role`,
  `sys_permission`, `sys_role_permission`) lands as Flyway migrations
  in `db/migration/iam/V100__iam_auth_tables.sql` and
  `V101__rbac_seed.sql`, so the *table contract* is pig-aligned even
  without pig code.
- SAS provides OAuth2 password / refresh_token / client_credentials
  grants out of the box; reimplementing pig's grant chain on top of
  SAS-with-opaque-tokens is simpler and easier to audit.

What pig is used for: **architecture reference only.** Concept
boundaries (auth, upms, gateway routing) are honored; pig commit ID is
pinned so that future contributors can compare against a known good
upstream snapshot.

## Runtime Surface (factual)

- Module: `mmpay-iam/` carries only a `pom.xml` placeholder; the IAM
  Java code lives under `mmpay-app/src/main/java/com/imgltd/mmpay/iam/`.
  This is a deliberate consequence of the SAS-direct decision.
- Tables: `oauth2_registered_client`, `oauth2_authorization`,
  `oauth2_authorization_consent`, `sys_user`, `sys_role`,
  `sys_user_role`, `sys_permission`, `sys_role_permission`.
- RBAC roles: `admin`, `ops`, `finance`, `auditor` plus
  `service-principal` kind (P1 §1.1.3).
- Endpoints: `/oauth2/token`, `/setup`, `/api/admin/*`,
  `/api/admin/audit/verify`.
- Sanitizer: `MmpayActuatorSanitizer` redacts `password / secret / key
  / token / signature / cert / pem / hmac / license_payload` in
  `/actuator/env` / `/actuator/configprops` / `/actuator/info`.

## What the v1.x roadmap MAY revisit

- A future minor release MAY introduce real pig vendoring under
  `mmpay-iam/` if multi-instance deploy or Nacos-managed config
  becomes a customer requirement. v1.0.0 GA does not need it.
- The literal "pig single-jar trim" wording in the P1 spec is
  superseded by this document.

## Compliance Reading

Public docs and external evidence MUST describe the backend IAM as
"Spring Authorization Server-direct, pig-aligned on table contract",
NOT "pig single-jar trim". The brand-allowlist scanner accepts
"pig-aligned" as governance language.
