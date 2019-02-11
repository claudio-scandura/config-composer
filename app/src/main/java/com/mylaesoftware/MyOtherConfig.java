package com.mylaesoftware;


@Config(rootKey = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atKey = "value")
  String getOtherConfigValue();

}
