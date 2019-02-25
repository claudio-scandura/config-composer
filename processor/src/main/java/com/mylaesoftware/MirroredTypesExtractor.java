package com.mylaesoftware;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.util.Types;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MirroredTypesExtractor {

  private final Types typeUtils;

  public MirroredTypesExtractor(Types typeUtils) {
    this.typeUtils = typeUtils;
  }

  public TypeElement extractType(Supplier<Class<?>> annotationParam) {
    try {
      annotationParam.get();
    } catch (MirroredTypeException mte) {
      return (TypeElement) typeUtils.asElement(mte.getTypeMirror());
    }
    throw new RuntimeException("Cannot find type element");
  }

  public List<TypeElement> extractTypes(Supplier<Class<?>[]> annotationParam) {
    try {
      annotationParam.get();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors().stream()
          .map(mirror -> (TypeElement) typeUtils.asElement(mirror))
          //TODO: Add validation of type parameter (needs to match annotated field type)
          .collect(Collectors.toList());
    }
    throw new RuntimeException("Cannot find type elements");
  }
}
