package com.mylaesoftware;


@Config
public interface MyConfig {

  @ConfigValue(atKey = "some.value")
  String getConfigValue();


}
