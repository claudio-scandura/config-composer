package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType(rootKey = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atKey = "value")
  String getConfigOtherValue();


  @ConfigValue(atKey = "value2", mapper = com.mylaesoftware.TestMapper.class)
  TestObject getTestObject();

}
