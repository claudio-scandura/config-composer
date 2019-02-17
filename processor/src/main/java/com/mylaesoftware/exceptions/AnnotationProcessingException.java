package com.mylaesoftware.exceptions;

import com.mylaesoftware.AbsoluteName;

import javax.lang.model.element.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class AnnotationProcessingException extends RuntimeException {

  private static final String MSG_FMT = "Wrong annotation usage in '%s'. %s";

  public AnnotationProcessingException(String message, Collection<Element> offendingElements) {
    super(String.format(MSG_FMT, toAbsoluteNames(offendingElements), message));
  }

  public AnnotationProcessingException(String message, Element offendingElement) {
    this(message, Collections.singletonList(offendingElement));
  }

  private static String toAbsoluteNames(Collection<Element> elements) {
    return elements.stream().map(AbsoluteName::of).collect(Collectors.joining(","));
  }
}
