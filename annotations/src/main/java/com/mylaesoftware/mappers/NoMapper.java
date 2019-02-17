package com.mylaesoftware.mappers;

import com.typesafe.config.Config;

import java.util.function.Function;

public class NoMapper implements ConfigMapper<Config> {

  @Override
  public Config apply(Config config, String key) {
    return Function.<Config>identity().apply(config);
  }
}
