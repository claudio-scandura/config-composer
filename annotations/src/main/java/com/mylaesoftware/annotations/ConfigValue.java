package com.mylaesoftware.annotations;

import com.mylaesoftware.mappers.ConfigMapper;
import com.mylaesoftware.mappers.NoMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigValue {

  String atKey();

  Class<? extends ConfigMapper<?>> mapper() default NoMapper.class;
}
