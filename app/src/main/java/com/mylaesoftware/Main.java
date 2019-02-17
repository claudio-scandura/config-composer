package com.mylaesoftware;

import com.mylaesoftware.annotations.ConfigType;
import com.typesafe.config.ConfigFactory;

public class Main {

  @ConfigType
  interface Config extends MyConfig, MyOtherConfig {
  }

  @ConfigType
  interface Bar { }

  public static void main(String[] args) {
    Config config = new GlobalConfig(ConfigFactory.load());

    System.out.println(config.getConfigValue() + " and " + config.getConfigOtherValue());
    System.out.println(config.getTestObject());
  }
}
