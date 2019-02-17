package com.mylaesoftware;

import com.typesafe.config.Config;

import java.util.function.BiFunction;

public class TestMapper implements BiFunction<Config, String, TestObject> {
  @Override
  public TestObject apply(Config config, String key) {
    return new TestObject(config.getString(key + ".fieldA"), config.getString(key + ".fieldB"));
  }
}
