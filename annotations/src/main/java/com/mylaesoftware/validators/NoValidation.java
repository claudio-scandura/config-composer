package com.mylaesoftware.validators;

import java.util.Collection;

import static java.util.Collections.emptySet;

public class NoValidation implements ConfigValidator<Object> {

  @Override
  public Collection<ValidationError> apply(Object object) {
    return emptySet();
  }
}
