package com.mylaesoftware.mappers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MappersTest {

  @Test
  public void configMapperShouldReturnTypesafeRootConfig() {
    Config config = ConfigFactory.parseString("foo { bar = 23 }");
    Assertions.assertThat(
        new BasicMappers.ConfigM().apply(config, "")
    ).isEqualTo(config);
  }

  @Test
  public void configMapperShouldReturnTypesafeConfigUnderGivenKey() {
    Config config = ConfigFactory.parseString("foo { bar = 23 }");
    Assertions.assertThat(
        new BasicMappers.ConfigM().apply(config, "foo")
    ).isEqualTo(ConfigFactory.parseString("bar = 23"));
  }

}
