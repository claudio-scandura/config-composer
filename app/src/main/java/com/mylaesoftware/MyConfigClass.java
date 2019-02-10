package com.mylaesoftware;



@Config
public interface MyConfigClass {

  @ConfigValue(atKey = "some.value")
  String getConfigValue();


}
