package com.mylaesoftware;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compilation.Status;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.mylaesoftware.assertions.DiagnosticAssert;
import com.mylaesoftware.specs.ConfigSpec;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.IOException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigProcessorTest {

  private static final String ANY_NAME = "[a-zA-Z0-9_]+";

  private static final String INPUT_SOURCE_NAME = "TestInterface";

  private static final String CONFIG_FIELD_KEY = "path.to.property";
  private static final String CONFIG_FIELD_TYPE = "String";
  private static final String CONFIG_FIELD_NAME = "property";
  private static final String NON_CONFIG_FIELD_NAME = "other";

  private static final String CONFIG_ANNOTATION = Config.class.getSimpleName();
  private static final String CONFIG_VALUE_ANNOTATION = ConfigValue.class.getSimpleName();

  private static final String DEFAULT_INPUT = String.format(
      "import com.mylaesoftware." + CONFIG_ANNOTATION + ";\n" +
          "import com.mylaesoftware." + CONFIG_VALUE_ANNOTATION + ";\n" +
          "\n" +
          "@" + CONFIG_ANNOTATION + "\n" +
          "interface %s {\n" +
          "\n" +
          "  @" + CONFIG_VALUE_ANNOTATION + "(atKey = \"%s\")\n" +
          "  %s %s();\n" +
          "  default String %s() {return \"\";}\n" +
          "\n" +
          "}", INPUT_SOURCE_NAME, CONFIG_FIELD_KEY, CONFIG_FIELD_TYPE, CONFIG_FIELD_NAME, NON_CONFIG_FIELD_NAME);

  private static void withCompiledSource(String inputSourceName, String inputSource, Consumer<Compilation> action) {
    action.accept(
        Compiler.javac()
            .withProcessors(new ConfigProcessor())
            .compile(JavaFileObjects.forSourceString(inputSourceName, inputSource))
    );
  }


  private static void withSuccessfulCompilation(String inputSource, Consumer<String> testBody) {
    withCompiledSource(INPUT_SOURCE_NAME, inputSource, result -> {
      try {
        assertThat(result.status())
            .as("Compilation failed with: %s", result.errors())
            .isEqualTo(Status.SUCCESS);

        assertThat(result.generatedSourceFiles()).hasSize(1);

        String generatedSource = IOUtils.toString(result.generatedSourceFiles().get(0).openInputStream(), UTF_8);
        testBody.accept(generatedSource);
      } catch (IOException e) {
        throw new AssertionError("Error while compiling source", e);
      }
    });
  }

  private static void withFailedCompilation(String inputSourceName, String inputSource,
                                            Consumer<ImmutableList<Diagnostic<? extends JavaFileObject>>> testBody) {
    withCompiledSource(inputSourceName, inputSource, result -> {

      assertThat(result.status())
          .as("Compilation should have failed")
          .isEqualTo(Status.FAILURE);

      testBody.accept(result.errors());
    });
  }

  @Nested
  class WhenSuccessfullyCompilingConfigInterface {

    @Test
    public void generateClassWithExpectedNameAndModifiers() {
      withSuccessfulCompilation(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource).contains("public final class " + ConfigSpec.CLASS_NAME)
      );

    }

    @Test
    public void generateClassThatDeclaresAndInitializesFinalFieldsForEachAnnotatedMethods() {

      withSuccessfulCompilation(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains("private final " + CONFIG_FIELD_TYPE + " " + CONFIG_FIELD_NAME)
              .containsPattern(CONFIG_FIELD_NAME + " = [^null].*")
              .doesNotContain(NON_CONFIG_FIELD_NAME)
      );

    }

    @Test
    public void generateClassThatImplementsInterfaceAndOverridesAnnotatedMethods() {

      withSuccessfulCompilation(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains(ConfigSpec.CLASS_NAME + " implements " + INPUT_SOURCE_NAME)
              .containsPattern("\\@Override(\\s+)public " + CONFIG_FIELD_TYPE + " " + CONFIG_FIELD_NAME + "\\(\\)")
              .contains("return " + CONFIG_FIELD_NAME)
      );

    }

    @Test
    public void generateClassThatImplementsStaticMethodToInitializePropertyFields() {

      withSuccessfulCompilation(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains("import com.typesafe.config.Config;")
              .containsPattern("private static " + CONFIG_FIELD_TYPE +
                  " " + ANY_NAME + capitalize(CONFIG_FIELD_NAME) + "\\((final )?Config " + ANY_NAME + "\\)")
              .containsPattern(
                  "return " + ANY_NAME + "\\.get" + CONFIG_FIELD_TYPE + "\\(\"" + CONFIG_FIELD_KEY + "\"\\)"
              )
      );

    }
  }

  @Nested
  class WhenFailingToCompileConfigInterface {

    @Test
    public void generateErrorIfConfigAnnotationUsedOnClasses() {
      String className = "Foo";
      String input = String.format(
          "import com.mylaesoftware." + CONFIG_ANNOTATION + ";\n" +
              "\n" +
              "@" + CONFIG_ANNOTATION + "\n" +
              "class %s {}\n", className);

      withFailedCompilation(className, input, errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining(CONFIG_ANNOTATION + " annotation can only be used on interfaces", className);
      });
    }

    @Test
    public void generateErrorIfConfigValueAnnotationUsedOnNonAnnotatedType() {
      String className = "Foo";
      String input = String.format(
          "import com.mylaesoftware." + CONFIG_VALUE_ANNOTATION + ";\n" +
              "\n" +
              "interface %s {\n" +
              "@" + CONFIG_VALUE_ANNOTATION + "(atKey = \"any\")\n" +
              "String stringValue();\n" +
              "}\n", className);

      withFailedCompilation(className, input, errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining(CONFIG_VALUE_ANNOTATION + " annotation needs to be used inside a " +
                CONFIG_ANNOTATION + " annotated type", className);
      });
    }
  }
}
