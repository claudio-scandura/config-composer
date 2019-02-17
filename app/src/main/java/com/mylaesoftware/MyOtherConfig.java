package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

@ConfigType(contextPath = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atPath = "value")
  String getConfigOtherValue();


  @ConfigValue(atPath = "value2", mapper = com.mylaesoftware.TestMapper.class)
  TestObject getTestObject();

}
