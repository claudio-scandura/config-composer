package com.mylaesoftware;

import com.mylaesoftware.mappers.ConfigMapper;
import com.typesafe.config.Config;

public class TestMapper implements ConfigMapper<TestObject> {
  @Override
  public TestObject apply(Config config, String key) {
    return new TestObject(config.getString(key + ".fieldA"), config.getString(key + ".fieldB"));
  }
}
