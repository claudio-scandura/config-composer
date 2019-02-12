package com.mylaesoftware;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compilation.Status;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.mylaesoftware.specs.ConfigSpec;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

  private static final String DEFAULT_INPUT = String.format("import com.mylaesoftware.Config;\n" +
      "import com.mylaesoftware.ConfigValue;\n" +
      "\n" +
      "@Config\n" +
      "interface %s {\n" +
      "\n" +
      "  @ConfigValue(atKey = \"%s\")\n" +
      "  %s %s();\n" +
      "  default String %s() {return \"\";}\n" +
      "\n" +
      "}", INPUT_SOURCE_NAME, CONFIG_FIELD_KEY, CONFIG_FIELD_TYPE, CONFIG_FIELD_NAME, NON_CONFIG_FIELD_NAME);

  private static void withCompiledSource(String inputSource, Consumer<String> testBody) throws IOException {
    Compilation result = Compiler.javac()
        .withProcessors(new ConfigProcessor())
        .compile(JavaFileObjects.forSourceString(INPUT_SOURCE_NAME, inputSource));

    assertThat(result.status())
        .as("Failed with: %s", result.errors())
        .isEqualTo(Status.SUCCESS);

    assertThat(result.generatedSourceFiles()).hasSize(1);

    String generatedSource = IOUtils.toString(result.generatedSourceFiles().get(0).openInputStream(), UTF_8);
    testBody.accept(generatedSource);
  }

  @Nested
  class WhenSuccessfullyCompilingConfigInterface {

    @Test
    public void generateClassWithExpectedNameAndModifiers() throws IOException {
      withCompiledSource(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource).contains("public final class " + ConfigSpec.CLASS_NAME)
      );

    }

    @Test
    public void generateClassThatDeclaresAndInitializesFinalFieldsForEachAnnotatedMethods() throws IOException {

      withCompiledSource(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains("private final " + CONFIG_FIELD_TYPE + " " + CONFIG_FIELD_NAME)
              .containsPattern(CONFIG_FIELD_NAME + " = [^null].*")
              .doesNotContain(NON_CONFIG_FIELD_NAME)
      );

    }

    @Test
    public void generateClassThatImplementsInterfaceAndOverridesAnnotatedMethods() throws IOException {

      withCompiledSource(DEFAULT_INPUT,
          actualSource -> assertThat(actualSource.replaceAll("\\n", " "))
              .contains(ConfigSpec.CLASS_NAME + " implements " + INPUT_SOURCE_NAME)
              .containsPattern("\\@Override(\\s+)public " + CONFIG_FIELD_TYPE + " " + CONFIG_FIELD_NAME + "\\(\\)")
              .contains("return " + CONFIG_FIELD_NAME)
      );

    }

    @Test
    public void generateClassThatImplementsStaticMethodToInitializePropertyFields() throws IOException {

      withCompiledSource(DEFAULT_INPUT,
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
}
