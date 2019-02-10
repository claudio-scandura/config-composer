package com.mylaesoftware;

@Config
interface TestInterface {

  @ConfigValue(atKey = "someKey")
  String getWhatever();

  default String doNotOverrideThis() {
    return "foo";
  }
}
