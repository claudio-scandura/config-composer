package com.mylaesoftware;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compilation.Status;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.mylaesoftware.specs.ConfigSpec;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigProcessorTest {

  private final static String INPUT_SOURCE_NAME = "TestInterface";

  private static String DEFAULT_INPUT = "import com.mylaesoftware.Config;\n" +
      "import com.mylaesoftware.ConfigValue;\n" +
      "\n" +
      "@Config\n" +
      "interface " + INPUT_SOURCE_NAME + " {\n" +
      "\n" +
      "  @ConfigValue(atKey = \"someKey\")\n" +
      "  String getWhatever();\n" +
      "\n" +
      "}";

  private static void withCompiledSource(String inputSource, Consumer<String> testBody) throws IOException {
    Compilation result = Compiler.javac()
        .withProcessors(new ConfigProcessor())
        .compile(JavaFileObjects.forSourceString(INPUT_SOURCE_NAME, inputSource));

    assertThat(result.status()).isEqualTo(Status.SUCCESS);
    assertThat(result.generatedSourceFiles()).hasSize(1);

    String generatedSource = IOUtils.toString(result.generatedSourceFiles().get(0).openInputStream(), UTF_8);
    testBody.accept(generatedSource);
  }

  @Test
  public void shouldGenerateClassWithExpectedNameAndModifiers() throws IOException {

    withCompiledSource(DEFAULT_INPUT,
        actualSource -> assertThat(actualSource).containsSequence("public final class " + ConfigSpec.CLASS_NAME)
    );

  }
}
