package com.mylaesoftware;

import com.mylaesoftware.subpackage.OtherTestInterface;
import org.junit.jupiter.api.Test;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {

  final class Stub {

    private Stub() {
    }

//    String readConfigValue(Config obj) {
//      return obj.getString("");
//    }
  }

  @Test
  public void shouldCreateImplementationClass() throws ClassNotFoundException {
    Class<?> result = GlobalConfig.class;
    assertThat(isFinal(result.getModifiers())).as("must be final").isTrue();
    assertThat(isPublic(result.getModifiers())).as("must be public").isTrue();
  }

  @Test
  public void shouldPlaceItInHighestPackage() throws ClassNotFoundException {
    Class<?> result = GlobalConfig.class;

    assertThat(result.getPackage().getName()).isEqualTo(getClass().getPackage().getName());
  }

  @Test
  public void shouldInheritAllInterfacesWithConfigAnnotation() throws ClassNotFoundException {
    Class<?> result = GlobalConfig.class;

    assertThat(result.getInterfaces()).containsExactlyInAnyOrder(TestInterface.class, OtherTestInterface.class);
    assertThat(result.getPackage().getName()).isEqualTo(getClass().getPackage().getName());
  }

}
