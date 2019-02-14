package com.mylaesoftware;

import com.typesafe.config.ConfigFactory;

public class Main {

  @com.mylaesoftware.Config
  interface Config extends MyConfig, MyOtherConfig {
  }

  @com.mylaesoftware.Config
  interface Bar { }

  public static void main(String[] args) {
    Config config = new GlobalConfig(ConfigFactory.load());

    System.out.println(config.getConfigValue() + " and " + config.getConfigOtherValue());
    System.out.println(config.getConfigValue() + " and " + config.getConfigOtherValue());
  }
}
