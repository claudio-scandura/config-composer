package com.mylaesoftware;


@ConfigType
public interface MyConfig {

  @ConfigValue(atKey = "some.value")
  String getConfigValue();


}
