package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AuditAppendOnlyTest {
  private static final Pattern FORBIDDEN_AUDIT_WRITE =
      Pattern.compile("(?is)(UPDATE\\s+audit_event|DELETE\\s+FROM\\s+audit_event|TRUNCATE\\s+audit_event)");

  @Test
  void productionSourcesNeverUpdateDeleteOrTruncateAuditEvents() throws Exception {
    var violations = productionFiles().stream().filter(AuditAppendOnlyTest::containsForbiddenWrite).toList();

    assertThat(violations).isEmpty();
  }

  private static List<Path> productionFiles() throws Exception {
    try (var paths = Files.walk(sourceRoot())) {
      return paths.filter(Files::isRegularFile).filter(AuditAppendOnlyTest::isScannedFile).toList();
    }
  }

  private static boolean containsForbiddenWrite(Path path) {
    try {
      return FORBIDDEN_AUDIT_WRITE.matcher(Files.readString(path)).find();
    } catch (Exception exception) {
      throw new IllegalStateException("audit_append_only_scan_failed: " + path, exception);
    }
  }

  private static boolean isScannedFile(Path path) {
    var fileName = path.getFileName().toString();
    return fileName.endsWith(".java") || fileName.endsWith(".sql");
  }

  private static Path sourceRoot() {
    var userDir = Path.of("").toAbsolutePath();
    var moduleRoot = userDir.resolve("src/main");
    return Files.exists(moduleRoot) ? moduleRoot : userDir.resolve("backend/mmpay-app/src/main");
  }
}
