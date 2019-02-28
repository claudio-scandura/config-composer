package com.mylaesoftware.example;

import com.mylaesoftware.validators.ConfigValidator;
import com.mylaesoftware.validators.ValidationError;

import java.util.Collection;
import java.util.Collections;

public class Validators {

  public static class NonEmptyCollection implements ConfigValidator<Collection<?>> {

    @Override
    public Collection<ValidationError> apply(Collection<?> objects) {
      return objects.isEmpty() ? Collections.singleton(new ValidationError("cannot be empty")) : Collections.emptySet();
    }
  }
}
