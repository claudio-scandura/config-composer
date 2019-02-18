package com.mylaesoftware;


import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;

import java.time.Duration;
import java.util.Optional;

@ConfigType
public interface MyConfig {

  @ConfigValue(atPath = "some.value")
  String getConfigValue();

  @ConfigValue(atPath = "some.duration")
  Optional<Duration> getDuration();

  @ConfigValue(atPath = "some.int")
  Optional<Integer> getInt();

}
