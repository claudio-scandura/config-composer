package com.mylaesoftware.example;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

import java.util.List;

@ConfigType(contextPath = "some.other")
public interface MyOtherConfig {

  @ConfigValue(atPath = "value")
  String getConfigOtherValue();

  @ConfigValue(atPath = "int")
  int getConfigIntValue();

  @ConfigValue(atPath = "double")
  double getDoubleValue();

  @ConfigValue(atPath = "list")
  List<String> getList();

  @ConfigValue(atPath = "int-list")
  List<Integer> getIntList();
}
