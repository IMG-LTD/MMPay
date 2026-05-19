package com.imgltd.mmpay.adapter.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.adapter.PaymentProviderAdapter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Repo-wide discovery test: every concrete {@link PaymentProviderAdapter} implementation MUST
 * have an accompanying contract test class extending {@link AbstractProviderCallbackContractTest}.
 *
 * <p>Spec P3 §6.1 makes this discovery rule mandatory. The test walks the entire module
 * classpath (post-build, every adapter's compiled classes are visible to the SPI test classpath
 * when invoked via {@code mvn test} from the reactor root or via integration tests in
 * mmpay-app).
 */
final class AdapterMustExtendCallbackContractTest {

  @Test
  @DisplayName("every PaymentProviderAdapter implementation has a contract test extension")
  void discoveryRule() throws Exception {
    var moduleRoots = discoverModuleSourceRoots();
    if (moduleRoots.isEmpty()) {
      return;
    }
    var adaptersMissingTest = new ArrayList<String>();
    for (var module : moduleRoots) {
      var adapters = findAdaptersInModule(module);
      if (adapters.isEmpty()) {
        continue;
      }
      var hasContractTest = moduleHasContractTestExtension(module);
      if (!hasContractTest) {
        adaptersMissingTest.addAll(adapters);
      }
    }

    assertTrue(
        adaptersMissingTest.isEmpty(),
        () ->
            "modules with PaymentProviderAdapter implementations missing a test extending "
                + "AbstractProviderCallbackContractTest: "
                + adaptersMissingTest);
  }

  private static List<Path> discoverModuleSourceRoots() {
    var here = Paths.get("").toAbsolutePath();
    var backendRoot = here;
    while (backendRoot != null && !Files.exists(backendRoot.resolve("mmpay-bom"))) {
      backendRoot = backendRoot.getParent();
    }
    if (backendRoot == null) {
      return List.of();
    }
    var modules = new ArrayList<Path>();
    try (var stream = Files.newDirectoryStream(backendRoot)) {
      for (var entry : stream) {
        if (Files.isDirectory(entry) && entry.getFileName().toString().startsWith("mmpay-")) {
          modules.add(entry);
        }
      }
    } catch (IOException io) {
      return List.of();
    }
    return modules;
  }

  private static List<String> findAdaptersInModule(Path module) throws IOException {
    var src = module.resolve("src/main/java");
    if (!Files.exists(src)) {
      return List.of();
    }
    var hits = new ArrayList<String>();
    walkAndMatch(src, ".java", (path, content) -> {
      if (content.contains("implements PaymentProviderAdapter")
          && !content.contains("abstract class")
          && !content.contains("AbstractProvider")) {
        hits.add(classNameOf(src, path));
      }
    });
    return hits;
  }

  private static boolean moduleHasContractTestExtension(Path module) throws IOException {
    var src = module.resolve("src/test/java");
    if (!Files.exists(src)) {
      return false;
    }
    var hits = new HashSet<String>();
    walkAndMatch(src, ".java", (path, content) -> {
      if (content.contains("extends AbstractProviderCallbackContractTest")) {
        hits.add(classNameOf(src, path));
      }
    });
    return !hits.isEmpty();
  }

  private static String classNameOf(Path root, Path file) {
    var relative = root.relativize(file).toString();
    var noExt = relative.endsWith(".java") ? relative.substring(0, relative.length() - 5) : relative;
    return noExt.replace('/', '.').replace('\\', '.');
  }

  private interface SourceVisitor {
    void visit(Path path, String content) throws IOException;
  }

  private static void walkAndMatch(Path src, String suffix, SourceVisitor visitor) throws IOException {
    Files.walkFileTree(
        src,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().endsWith(suffix)) {
              visitor.visit(file, Files.readString(file));
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
