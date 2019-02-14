package com.mylaesoftware;


@Config(rootKey = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atKey = "value")
  String getConfigOtherValue();

  @ConfigValue(atKey = "value2")
  String getConfigValue2();

}
