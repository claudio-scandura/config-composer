package com.mylaesoftware.assertions;

import org.assertj.core.api.ObjectAssert;

import javax.tools.JavaFileObject;

import java.util.Locale;
import java.util.stream.Stream;

public class DiagnosticAssert<T extends JavaFileObject> extends ObjectAssert<javax.tools.Diagnostic> {

  public DiagnosticAssert(javax.tools.Diagnostic actual) {
    super(actual);
  }

  public static <T extends JavaFileObject> DiagnosticAssert assertThat(javax.tools.Diagnostic actual) {
    return new DiagnosticAssert<>(actual);
  }

  public DiagnosticAssert<T> isErrorContaining(String... substrings) {
    isNotNull();
    hasFieldOrPropertyWithValue("kind", javax.tools.Diagnostic.Kind.ERROR);

    final String error = actual.getMessage(Locale.ENGLISH);
    Stream.of(substrings).forEach(substring -> {
      if (!error.contains(substring)) {
        failWithMessage("Expected [%s] to contain substring [%s]", error, substring);
      }
    });

    return this;
  }

}
