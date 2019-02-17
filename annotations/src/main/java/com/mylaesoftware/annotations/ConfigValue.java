package com.mylaesoftware.annotations;

import com.mylaesoftware.NoMapper;
import com.typesafe.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiFunction;


@Target( {ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigValue {

  String atKey();

  Class<? extends BiFunction<Config, String, ?>> mapper() default NoMapper.class;
}
