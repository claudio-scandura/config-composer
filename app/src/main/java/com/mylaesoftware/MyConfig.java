package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

import java.time.Duration;

@ConfigType
public interface MyConfig {

  @ConfigValue(atKey = "some.value")
  String getConfigValue();

  @ConfigValue(atKey = "some.duration")
  Duration getDuration();

}
