package com.mylaesoftware.mappers;

import com.typesafe.config.Config;

import java.time.Duration;
import java.util.List;

public class CollectionsMappers {

  private CollectionsMappers() {
  }

  public static final class StringListM implements ConfigMapper<List<String>> {
    @Override
    public List<String> apply(Config config, String key) {
      return config.getStringList(key);
    }
  }

  public static final class DurationListM implements ConfigMapper<List<Duration>> {
    @Override
    public List<Duration> apply(Config config, String key) {
      return config.getDurationList(key);
    }
  }

  public static final class BooleanListM implements ConfigMapper<List<Boolean>> {
    @Override
    public List<Boolean> apply(Config config, String key) {
      return config.getBooleanList(key);
    }
  }

  public static final class IntListM implements ConfigMapper<List<Integer>> {
    @Override
    public List<Integer> apply(Config config, String key) {
      return config.getIntList(key);
    }
  }

  public static final class LongListM implements ConfigMapper<List<Long>> {
    @Override
    public List<Long> apply(Config config, String key) {
      return config.getLongList(key);
    }
  }

  public static final class NumberListM implements ConfigMapper<List<Number>> {
    @Override
    public List<Number> apply(Config config, String key) {
      return config.getNumberList(key);
    }
  }

  public static final class DoubleListM implements ConfigMapper<List<Double>> {
    @Override
    public List<Double> apply(Config config, String key) {
      return config.getDoubleList(key);
    }
  }

  public static final class ConfigListM implements ConfigMapper<List<? extends Config>> {
    @Override
    public List<? extends Config> apply(Config config, String key) {
      return config.getConfigList(key);
    }
  }

  public static final class AnyRefListM implements ConfigMapper<List<?>> {
    @Override
    public List<?> apply(Config config, String key) {
      return config.getAnyRefList(key);
    }
  }
}
