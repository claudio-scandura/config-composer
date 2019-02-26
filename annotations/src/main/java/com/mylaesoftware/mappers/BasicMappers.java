package com.mylaesoftware.mappers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigValue;

import java.time.Duration;

public class BasicMappers {

  private BasicMappers() {
  }

  public static final class StringM implements ConfigMapper<String> {
    @Override
    public String apply(Config config, String key) {
      return config.getString(key);
    }
  }

  public static final class DurationM implements ConfigMapper<Duration> {
    @Override
    public Duration apply(Config config, String key) {
      return config.getDuration(key);
    }
  }

  public static final class BooleanM implements ConfigMapper<Boolean> {
    @Override
    public Boolean apply(Config config, String key) {
      return config.getBoolean(key);
    }
  }

  public static final class IntM implements ConfigMapper<Integer> {
    @Override
    public Integer apply(Config config, String key) {
      return config.getInt(key);
    }
  }

  public static final class LongM implements ConfigMapper<Long> {
    @Override
    public Long apply(Config config, String key) {
      return config.getLong(key);
    }
  }

  public static final class NumberM implements ConfigMapper<Number> {
    @Override
    public Number apply(Config config, String key) {
      return config.getNumber(key);
    }
  }

  public static final class DoubleM implements ConfigMapper<Double> {
    @Override
    public Double apply(Config config, String key) {
      return config.getDouble(key);
    }
  }

  public static final class ConfigValueM implements ConfigMapper<ConfigValue> {
    @Override
    public ConfigValue apply(Config config, String key) {
      return config.getValue(key);
    }
  }

  public static final class ConfigM implements ConfigMapper<Config> {
    @Override
    public Config apply(Config config, String key) {
      return config.getConfig(key);
    }
  }

  public static final class AnyRefM implements ConfigMapper<Object> {
    @Override
    public Object apply(Config config, String key) {
      return config.getAnyRef(key);
    }
  }

  public static final class EnumM<T extends Enum<T>> implements ConfigMapper<T> {
    private final Class<T> clazz;

    public EnumM(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T apply(Config config, String key) {
      return config.getEnum(clazz, key);
    }
  }

  public static final class BeanM<T> implements ConfigMapper<T> {
    private final Class<T> clazz;

    public BeanM(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T apply(Config config, String key) {
      return ConfigBeanFactory.create(config, clazz);
    }
  }
}
