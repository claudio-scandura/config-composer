package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

import java.util.List;

@ConfigType(contextPath = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atPath = "value")
  String getConfigOtherValue();

  @ConfigValue(atPath = "list")
  List<String> getList();


  @ConfigValue(atPath = "value2", mappedBy = com.mylaesoftware.TestMapper.class)
  TestObject getTestObject();

}
