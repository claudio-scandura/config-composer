package com.mylaesoftware;

import com.typesafe.config.Config;

import java.util.function.BiFunction;
import java.util.function.Function;

public class NoMapper implements BiFunction<Config, String, Config> {

  @Override
  public Config apply(Config config, String key) {
    return Function.<Config>identity().apply(config);
  }
}
