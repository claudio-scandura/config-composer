package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType
public interface MyConfig {

  @ConfigValue(atKey = "some.value")
  String getConfigValue();

}
