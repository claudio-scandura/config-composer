package com.mylaesoftware.validators;

import java.util.Collection;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class NonEmptyString implements ConfigValidator<String> {
  @Override
  public Collection<ValidationError> apply(String s) {
    return s.trim().isEmpty() ? singleton(new ValidationError("cannot be empty")) : emptySet();
  }
}
