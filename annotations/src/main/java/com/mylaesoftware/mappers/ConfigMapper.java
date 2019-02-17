package com.mylaesoftware.mappers;

import com.typesafe.config.Config;

import java.util.function.BiFunction;

@FunctionalInterface
public interface ConfigMapper<T> extends BiFunction<Config, String, T> {
}
