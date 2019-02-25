package com.mylaesoftware.validators;

import java.util.Collection;
import java.util.stream.Collectors;

public class ConfigValidationException extends RuntimeException {

  public ConfigValidationException(Collection<ValidationError> errors) {
    super("Config validation failed with the following errors:\n"
        + errors.stream()
        .map(ConfigValidationException::formatted)
        .collect(Collectors.joining("\n", "<<--\n", "\n-->>")));
  }

  private static String formatted(ValidationError error) {
    return "|\t* " + error.toString();
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

}
