package com.mylaesoftware;

@ConfigType
interface TestInterface {

  @ConfigValue(atKey = "someKey")
  String getWhatever();

  default String doNotOverrideThis() {
    return "foo";
  }
}
