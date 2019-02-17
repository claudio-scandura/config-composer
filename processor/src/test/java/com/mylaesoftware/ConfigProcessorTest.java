package com.mylaesoftware;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compilation.Status;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.mylaesoftware.assertions.DiagnosticAssert;
import com.mylaesoftware.specs.ConfigTypeSpec;
import com.typesafe.config.Config;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mylaesoftware.Annotations.CONFIG_TYPE;
import static com.mylaesoftware.Annotations.CONFIG_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigProcessorTest {

  private static final String ANY_NAME = "[a-zA-Z0-9_]+";

  private static final String INPUT_SOURCE_NAME = "TestInterface";

  private static final String CONFIG_FIELD_KEY = "path.to.property";
  private static final String CONFIG_FIELD_TYPE = "String";
  private static final String CONFIG_FIELD_NAME = "property";
  private static final String NON_CONFIG_FIELD_NAME = "other";


  private static final String DEFAULT_INPUT = String.format(
      "import " + CONFIG_TYPE.canonicalName + ";\n" +
          "import " + CONFIG_VALUE.canonicalName + ";\n" +
          "\n" +
          "@" + CONFIG_TYPE.name + "\n" +
          "interface %s {\n" +
          "\n" +
          "  @" + CONFIG_VALUE.name + "(atPath = \"%s\")\n" +
          "  %s %s();\n" +
          "  default String %s() {return \"\";}\n" +
          "\n" +
          "}", INPUT_SOURCE_NAME, CONFIG_FIELD_KEY, CONFIG_FIELD_TYPE, CONFIG_FIELD_NAME, NON_CONFIG_FIELD_NAME);

  private static void withCompiledSource(Map<String, String> inputSources, Consumer<Compilation> action) {
    action.accept(
        Compiler.javac()
            .withProcessors(new ConfigProcessor())
            .compile(
                inputSources.entrySet().stream()
                    .map(kv -> JavaFileObjects.forSourceString(kv.getKey(), kv.getValue()))
                    .collect(Collectors.toList())
            )
    );
  }


  private static void withSuccessfulCompilation(String inputSource, Consumer<String> testBody) {
    withCompiledSource(singletonMap(INPUT_SOURCE_NAME, inputSource), result -> {
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

  private static void withFailedCompilation(Map<String, String> inputSources,
                                            Consumer<ImmutableList<Diagnostic<? extends JavaFileObject>>> testBody) {
    withCompiledSource(inputSources, result -> {

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
          actualSource -> assertThat(actualSource).contains("public final class " + ConfigTypeSpec.CLASS_NAME)
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
              .contains(ConfigTypeSpec.CLASS_NAME + " implements " + INPUT_SOURCE_NAME)
              .containsPattern("\\@Override(\\s+)public " + CONFIG_FIELD_TYPE + " " + CONFIG_FIELD_NAME + "\\(\\)")
              .contains("return " + CONFIG_FIELD_NAME)
      );

    }

    @Test
    public void generateClassThatImplementsStaticMethodToInitializePropertyFields() {

      withSuccessfulCompilation(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains("import " + Config.class.getCanonicalName())
              .containsPattern("private static " + CONFIG_FIELD_TYPE +
                  " " + ANY_NAME + capitalize(CONFIG_FIELD_NAME) + "\\((final )?Config " + ANY_NAME + "\\)")
              .containsPattern(
                  "return new [a-zA-Z0-9_\\.]+\\(\\)\\.apply\\(" + ANY_NAME + ", \"" + CONFIG_FIELD_KEY + "\"\\)"
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
          "import " + CONFIG_TYPE.canonicalName + ";\n" +
              "\n" +
              "@" + CONFIG_TYPE.name + "\n" +
              "class %s {}\n", className);

      withFailedCompilation(singletonMap(className, input), errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining(CONFIG_TYPE.name + " annotation can only be used on interfaces", className);
      });
    }

    @Test
    public void generateErrorIfConfigValueAnnotationUsedOnNonAnnotatedType() {
      String className = "Foo";
      String methodName = "stringValue";
      String input = String.format(
          "import " + CONFIG_VALUE.canonicalName + ";\n" +
              "\n" +
              "interface %s {\n" +
              "@" + CONFIG_VALUE.name + "(atPath = \"any\")\n" +
              "String %s();\n" +
              "}\n", className, methodName);

      withFailedCompilation(singletonMap(className, input), errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining(CONFIG_VALUE.name + " needs to be enclosed by a type annotated with " +
                CONFIG_TYPE.name, className, methodName);
      });
    }

    @Test
    public void generateErrorIfNonAnnotatedMethodInConfigInterfaceHasNoDefaultImplementation() {
      String interfaceName = "Foo";
      String methodName = "stringValue";
      String input = String.format(
          "import " + CONFIG_TYPE.canonicalName + ";\n" +
              "\n" +
              "@" + CONFIG_TYPE.name + "\n" +
              "interface %s {\n" +
              "\n" +
              "String %s();\n" +
              "}\n", interfaceName, methodName);

      withFailedCompilation(singletonMap(interfaceName, input), errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining("Abstract method ", "needs to be annotated with " + CONFIG_VALUE.name,
                "or have default implementation", interfaceName, methodName);
      });
    }

    @Test
    public void generateErrorIfAnnotatedMethodHasUnsupportedReturnedTypeAndNoCustomMapperIsGiven() {
      String interfaceName = "Foo";
      String methodName = "stringValue";
      String unsupportedType = Date.class.getCanonicalName();
      String input = String.format(
          "import " + CONFIG_TYPE.canonicalName + ";\n" +
              "import " + CONFIG_VALUE.canonicalName + ";\n" +
              "\n" +
              "@" + CONFIG_TYPE.name + "\n" +
              "interface %s {\n" +
              "@" + CONFIG_VALUE.name + "(atPath = \"any\")\n" +
              "%s %s();\n" +
              "}\n", interfaceName, unsupportedType, methodName);

      withFailedCompilation(singletonMap(interfaceName, input), errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining("Unsupported config value type", unsupportedType, "provide a custom mapper",
                interfaceName, methodName);
      });
    }

    @Test
    public void generateErrorIfAnnotatedMethodsNameHaveDuplicates() {
      String interfaceName = "Foo";
      String input = String.format(
          "import " + CONFIG_TYPE.canonicalName + ";\n" +
              "import " + CONFIG_VALUE.canonicalName + ";\n" +
              "\n" +
              "@" + CONFIG_TYPE.name + "\n" +
              "interface %s {\n" +
              "@" + CONFIG_VALUE.name + "(atPath = \"any\")\n" +
              "%s %s();\n" +
              "}\n", interfaceName, CONFIG_FIELD_TYPE, CONFIG_FIELD_NAME);

      Map<String, String> sources = new HashMap<>();
      sources.put(interfaceName, input);
      sources.put(INPUT_SOURCE_NAME, DEFAULT_INPUT);
      withFailedCompilation(sources, errors -> {
        assertThat(errors).hasSize(1);
        DiagnosticAssert.assertThat(errors.get(0))
            .isErrorContaining("cannot be used on multiple methods with the same name",
                CONFIG_FIELD_NAME, CONFIG_VALUE.name, interfaceName);
      });
    }
  }
}
