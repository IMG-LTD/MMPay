import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { constants } from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const forbiddenDependencyPattern = /(nacos|sentinel|alibaba|gateway-server|feign)/i;

async function exists(relativePath) {
  await access(path.join(root, relativePath), constants.F_OK);
}

async function read(relativePath) {
  return readFile(path.join(root, relativePath), 'utf8');
}

describe('P1 foundation contract', () => {
  it('adds the IAM module and removes Pig/Spring Cloud Alibaba runtime dependencies', async () => {
    const backendPom = await read('backend/pom.xml');
    const appPom = await read('backend/mmpay-app/pom.xml');

    await exists('backend/mmpay-iam/pom.xml');
    assert.match(backendPom, /<module>mmpay-iam<\/module>/);
    assert.doesNotMatch(backendPom, forbiddenDependencyPattern);
    assert.doesNotMatch(appPom, forbiddenDependencyPattern);
  });

  it('uses the P1 module-prefixed Flyway topology', async () => {
    const appConfig = await read('backend/mmpay-app/src/main/resources/application.yml');

    await exists('backend/mmpay-gateway-core/src/main/resources/db/migration/gateway/V001__create_payment_core.sql');
    await exists('backend/mmpay-app/src/main/resources/db/migration/iam/V100__iam_auth_tables.sql');
    await exists('backend/mmpay-app/src/main/resources/db/migration/iam/V101__rbac_seed.sql');
    await exists('backend/mmpay-app/src/main/resources/db/migration/audit/V200__audit_event.sql');
    await exists('backend/mmpay-app/src/main/resources/db/migration/audit/V201__audit_role_grants.sql');
    await exists('backend/mmpay-app/src/main/resources/db/migration/audit/V202__audit_chain_lock_key.sql');
    assert.match(appConfig, /classpath:db\/migration\/iam/);
    assert.match(appConfig, /classpath:db\/migration\/gateway/);
    assert.match(appConfig, /classpath:db\/migration\/audit/);
    assert.match(appConfig, /include: health,info,env,configprops,metrics/);
    assert.match(appConfig, /SET ROLE mmpay_app_role/);
  });

  it('ships P1 audit, resolver, setup, and sanitizer source boundaries', async () => {
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/AuditConfiguration.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/IamAuthorizationServerConfiguration.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/SecurityConfiguration.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditAdminController.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditAppendRequest.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditChain.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditDetails.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditEventStore.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditHasher.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditLock.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditWriter.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditVerifier.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/JdbcAuditEventStore.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/JdbcAuditEventStoreConfig.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/PostgresAuditLock.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/credentials/EnvironmentReferenceResolver.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/JdbcIamRepository.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/OAuthTokenController.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/OpaqueBearerTokenFilter.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/OpaqueTokenService.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/ServicePrincipalController.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/ServicePrincipalService.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupTokenService.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupController.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/JdbcSetupAdminRepository.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/PostgresSetupBootstrapLock.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupAdminFactory.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupAlreadyCompletedException.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupBootstrapLock.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupBootstrapService.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupBootstrapStore.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupRateLimiter.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupStartupTokenPrinter.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupPageCopy.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/BootstrapAdminInitializer.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/BootstrapAdminProperties.java');
    await exists('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/MmpayActuatorSanitizer.java');
    await exists('backend/mmpay-app/src/main/resources/templates/setup.html');
  });

  it('wires P1 RBAC, audit API, and setup route contracts into local validation', async () => {
    const backendPom = await read('backend/pom.xml');
    const appPom = await read('backend/mmpay-app/pom.xml');
    const validateLocal = await read('scripts/validate-local.sh');
    const securityConfig = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/SecurityConfiguration.java');
    const authorizationServerConfig = await read(
      'backend/mmpay-app/src/main/java/com/imgltd/mmpay/app/IamAuthorizationServerConfiguration.java'
    );
    const auditController = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/AuditAdminController.java');
    const auditStore = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/JdbcAuditEventStore.java');
    const auditLock = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/audit/PostgresAuditLock.java');
    const iamRepository = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/JdbcIamRepository.java');
    const tokenController = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/OAuthTokenController.java');
    const tokenService = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/OpaqueTokenService.java');
    const iamMigration = await read('backend/mmpay-app/src/main/resources/db/migration/iam/V100__iam_auth_tables.sql');
    const servicePrincipalController = await read(
      'backend/mmpay-app/src/main/java/com/imgltd/mmpay/iam/ServicePrincipalController.java'
    );
    const setupController = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupController.java');
    const setupRepository = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/JdbcSetupAdminRepository.java');
    const setupLock = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/PostgresSetupBootstrapLock.java');
    const rateLimiter = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupRateLimiter.java');
    const startupTokenPrinter = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/SetupStartupTokenPrinter.java');
    const bootstrapInitializer = await read('backend/mmpay-app/src/main/java/com/imgltd/mmpay/setup/BootstrapAdminInitializer.java');
    const setupTemplate = await read('backend/mmpay-app/src/main/resources/templates/setup.html');

    assert.doesNotMatch(backendPom, /MMPAY_AUDIT_HMAC_KEY/);
    assert.match(appPom, /spring-boot-starter-security/);
    assert.match(appPom, /spring-security-oauth2-authorization-server/);
    assert.match(appPom, /spring-boot-starter-jdbc/);
    assert.match(appPom, /spring-boot-starter-thymeleaf/);
    assert.match(validateLocal, /AuditAppendOnlyTest/);
    assert.match(validateLocal, /ActuatorHardeningTest/);
    assert.match(validateLocal, /P1AdminSecurityContractTest/);
    assert.match(validateLocal, /P1SetupRouteLifecycleTest/);
    assert.match(validateLocal, /P1SetupRateLimitTest/);
    assert.match(validateLocal, /P1BootstrapAdminInitializerTest/);
    assert.match(validateLocal, /P1SetupStartupTokenLogTest/);
    assert.match(validateLocal, /P1SetupSingleFlightTest/);
    assert.match(validateLocal, /P1SetupThymeleafTemplateTest/);
    assert.match(validateLocal, /JdbcAuditEventStoreTest/);
    assert.match(validateLocal, /AuditChainSerializationTest/);
    assert.match(validateLocal, /SpringAuthorizationServerJdbcWiringTest/);
    assert.match(validateLocal, /ServicePrincipalGrantTest/);
    assert.match(validateLocal, /PasswordGrantTokenTest/);
    assert.doesNotMatch(securityConfig, /oauth_password_grant_not_wired/);
    assert.match(securityConfig, /OpaqueBearerTokenFilter/);
    assert.match(securityConfig, /\/actuator\/env\/\*\*/);
    assert.match(securityConfig, /hasRole\("ADMIN"\)/);
    assert.match(securityConfig, /\/api\/admin\/audit/);
    assert.match(auditController, /\/api\/admin\/audit\/verify/);
    assert.match(auditController, /hasAnyRole\('ADMIN','AUDITOR'\)/);
    assert.match(auditStore, /@Transactional/);
    assert.match(auditStore, /lock\.withLock/);
    assert.match(auditStore, /SELECT row_hmac FROM audit_event ORDER BY id DESC LIMIT 1/);
    assert.match(auditStore, /SELECT nextval\('audit_event_id_seq'\)/);
    assert.match(auditLock, /AUDIT_EVENT_CHAIN_LOCK_ID = 7341L/);
    assert.match(auditLock, /pg_advisory_xact_lock/);
    assert.match(iamRepository, /oauth2_registered_client/);
    assert.match(iamRepository, /oauth2_authorization/);
    assert.match(authorizationServerConfig, /JdbcRegisteredClientRepository/);
    assert.match(authorizationServerConfig, /JdbcOAuth2AuthorizationService/);
    assert.match(iamMigration, /client_authentication_methods/);
    assert.match(iamMigration, /client_settings/);
    assert.match(iamMigration, /access_token_metadata/);
    assert.match(tokenController, /\/oauth2\/token/);
    assert.match(tokenService, /client_credentials/);
    assert.match(tokenService, /password/);
    assert.match(tokenService, /refresh_token/);
    assert.match(servicePrincipalController, /\/api\/admin\/service-principals/);
    assert.match(servicePrincipalController, /hasRole\('ADMIN'\)/);
    assert.match(setupController, /\/setup/);
    assert.match(setupController, /X-Setup-Token/);
    assert.match(setupController, /TOO_MANY_REQUESTS/);
    assert.match(setupController, /SetupPageCopy\.from\(locale\)/);
    assert.match(setupController, /return "setup"/);
    assert.match(setupRepository, /INSERT INTO sys_user/);
    assert.match(setupRepository, /INSERT INTO sys_user_role/);
    assert.match(setupLock, /IAM_BOOTSTRAP_LOCK_ID = 7340L/);
    assert.match(setupLock, /pg_advisory_xact_lock/);
    assert.match(rateLimiter, /MAX_POSTS_PER_WINDOW = 5/);
    assert.match(startupTokenPrinter, /System\.out\.println\(tokenService\.stdoutLine\(\)\)/);
    assert.match(bootstrapInitializer, /properties\.configured\(\)/);
    assert.match(bootstrapInitializer, /repository\.adminExists\(\)/);
    assert.match(setupTemplate, /th:action="@\{\/setup\}"/);
    assert.match(setupTemplate, /th:lang="\$\{copy\.lang\(\)\}"/);
    assert.match(setupTemplate, /name="passwordConfirm"/);
  });

  it('runs P1 governance scanners from local and CI validation', async () => {
    const validateLocal = await read('scripts/validate-local.sh');
    const validateCi = await read('scripts/validate-ci.sh');
    const packageJson = await read('frontend-admin/package.json');
    const appEnv = await read('frontend-admin/.env');

    await exists('scripts/governance/brand-neutrality-scan.sh');
    await exists('governance/brand-allowlist.yaml');
    await exists('governance/webhook-out-five-field-scan.sh');
    await exists('frontend-admin/scripts/mmpay-frontend-contract.mjs');
    assert.match(validateLocal, /brand-neutrality-scan\.sh/);
    assert.match(validateLocal, /webhook-out-five-field-scan\.sh/);
    assert.match(validateCi, /brand-neutrality-scan\.sh|validate-local\.sh/);
    assert.match(packageJson, /mmpay-frontend-contract\.mjs/);
    assert.doesNotMatch(packageJson, /mmpay-soybean-contract\.mjs/);
    assert.doesNotMatch(appEnv, /soybean-admin/i);
  });

  it('finishes the P1 frontend de-scaffold inventory', async () => {
    const frontendReadme = await read('frontend-admin/README.md');
    const enUs = await read('frontend-admin/src/locales/langs/en-us.ts');
    const zhCn = await read('frontend-admin/src/locales/langs/zh-cn.ts');
    const preset = await read('frontend-admin/src/theme/preset/preset-a.json');

    await assert.rejects(() => exists('frontend-admin/src/views/home/modules/project-news.vue'));
    await assert.rejects(() => exists('frontend-admin/src/theme/preset/azir.json'));
    assert.doesNotMatch(frontendReadme, /soybean|mmpay-soybean-contract/i);
    assert.doesNotMatch(enUs, /Azir/i);
    assert.doesNotMatch(zhCn, /Azir/i);
    assert.match(preset, /theme\.appearance\.preset\.presetA/);
  });
});
