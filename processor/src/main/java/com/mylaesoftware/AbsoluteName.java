package com.mylaesoftware;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbsoluteName {

  private AbsoluteName() {
  }

  public static String of(Element element) {
    return getEnclosingElements(element)
        .map(toName)
        .collect(Collectors.joining())
        .substring(1);
  }

  private static final Function<Element, String> toName = element -> {
    String name = element.getSimpleName().toString().trim();
    if (element.getKind().equals(ElementKind.METHOD)) {
      return "::" + name;
    }
    return name.isEmpty() ? name : "." + name;
  };


  private static Stream<Element> getEnclosingElements(Element e) {
    return getEnclosingElements(e, Stream.of(e));
  }

  private static Stream<Element> getEnclosingElements(Element e, Stream<Element> enclosing) {
    Element enclosingElement = e.getEnclosingElement();
    if (enclosingElement == null) {
      return enclosing;
    }
    return getEnclosingElements(enclosingElement, Stream.concat(Stream.of(enclosingElement), enclosing));
  }
}
