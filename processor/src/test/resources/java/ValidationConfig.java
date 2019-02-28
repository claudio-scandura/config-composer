package com.mylaesoftware.example;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.example.Validators.NonEmptyCollection;

import java.util.List;

@ConfigType(contextPath = "validation")
public interface ValidationConfig {

  @ConfigValue(atPath = "strings", validatedBy = com.mylaesoftware.example.Validators.NonEmptyCollection.class)
  List<String> strings();

  @ConfigValue(atPath = "numbers", validatedBy = NonEmptyCollection.class)
  List<String> numbers();

}
