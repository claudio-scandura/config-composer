package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

import java.time.Duration;

@ConfigType
public interface MyConfig {

  @ConfigValue(atPath = "some.value")
  String getConfigValue();

  @ConfigValue(atPath = "some.duration")
  Duration getDuration();

  @ConfigValue(atPath = "some.int")
  int getInt();

  @ConfigValue(atPath = "some.integer")
  Integer getInteger();
}
