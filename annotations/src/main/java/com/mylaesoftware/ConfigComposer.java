package com.mylaesoftware;

import com.typesafe.config.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ConfigComposer {

  private static final String CONFIG_CLASS_NAME = String.format(
      "%s.%s",
      GlobalConfig.class.getPackage().getName(),
      GlobalConfig.IMPLEMENTATION_NAME
  );

  private ConfigComposer() {

  }

  @SuppressWarnings("unchecked")
  public static <C extends GlobalConfig> C wire(Config config) {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<C> implementation = (Class<C>) loader.loadClass(CONFIG_CLASS_NAME);
      Constructor<C> constructor = implementation.getDeclaredConstructor(Config.class);
      return constructor.newInstance(config);
    } catch (ClassNotFoundException
        | NoSuchMethodException
        | IllegalAccessException
        | InstantiationException
        | InvocationTargetException e) {
      throw new RuntimeException("Error while loading config", e);
    }
  }
}
