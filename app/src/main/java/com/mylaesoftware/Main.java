package com.mylaesoftware;

import com.typesafe.config.ConfigFactory;

public class Main {

  @Config
  interface MyConfig extends MyConfigClass, MyOtherConfig {
  }

  public static void main(String[] args) {
    MyConfig config = new GlobalConfig(ConfigFactory.load());

    System.out.println(config.getConfigValue() + " and " + config.getOtherConfigValue());
  }
}
