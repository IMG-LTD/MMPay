package com.imgltd.mmpay.license;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Spec §1.1.3 layer 1: ArchUnit module rule. The relay module is intentionally walled to a small
 * dependency surface so a contributor cannot transitively reach a JSON parser, an AOP advisor, a
 * shared HTTP client, or a coroutine runtime through a routine PR.
 */
class LicenseRelayModuleBoundaryArchTest {
  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.imgltd.mmpay.license");
  }

  @Test
  void relayModuleOnlyDependsOnAllowedRoots() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("com.imgltd.mmpay.license..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.imgltd.mmpay.license..",
                "java..",
                "javax.crypto..",
                "javax.net.ssl..",
                "org.slf4j..");
    rule.check(classes);
  }

  @Test
  void relayModuleDoesNotImportJsonParsers() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.imgltd.mmpay.license..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.fasterxml.jackson..",
                "org.yaml.snakeyaml..",
                "com.google.protobuf..",
                "com.google.gson..",
                "com.squareup.moshi..",
                "kotlinx.serialization..");
    rule.check(classes);
  }

  @Test
  void relayModuleDoesNotImportAopOrReactor() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("com.imgltd.mmpay.license..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "org.aspectj..",
                "org.springframework.aop..",
                "io.projectreactor..",
                "io.netty..",
                "okhttp3..",
                "org.apache.http..",
                "org.apache.hc..");
    rule.check(classes);
  }
}
