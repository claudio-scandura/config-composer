package com.mylaesoftware;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

public enum Annotations {

  CONFIG_TYPE(ConfigType.class),
  CONFIG_VALUE(ConfigValue.class);

  private final Class<?> annotation;
  public final String name;
  public final String canonicalName;

  Annotations(Class<?> clazz) {
    name = clazz.getSimpleName();
    canonicalName = clazz.getCanonicalName();
    annotation = clazz;
  }

  @SuppressWarnings("unchecked")
  <T> Class<T> annotation() {
    return (Class<T>) annotation;
  }

}
