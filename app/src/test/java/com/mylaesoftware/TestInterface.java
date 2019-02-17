package com.mylaesoftware;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType
interface TestInterface {

  @ConfigValue(atKey = "someKey")
  String getWhatever();

  default String doNotOverrideThis() {
    return "foo";
  }
}
