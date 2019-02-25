package com.mylaesoftware;

import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.NoValidation;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class AnnotationParamExtractor {

  private static final Set<String> IGNORED_TYPES = Stream.of(NoMapper.class, NoValidation.class)
      .map(Class::getCanonicalName)
      .collect(toSet());

  private static final Predicate<TypeElement> NOT_IGNORED = element ->
      !IGNORED_TYPES.contains(element.getQualifiedName().toString());

  private final Types typeUtils;

  public AnnotationParamExtractor(Types typeUtils) {
    this.typeUtils = typeUtils;
  }

  public Optional<TypeElement> extractElement(Supplier<Class<?>> annotationParam) {
    try {
      annotationParam.get();
    } catch (MirroredTypeException mte) {
      return Optional.of((TypeElement) typeUtils.asElement(mte.getTypeMirror()))
          .filter(NOT_IGNORED);
    }
    throw new RuntimeException("Cannot find type element");
  }

  public List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam) {
    try {
      annotationParam.get();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors().stream()
          .map(mirror -> (TypeElement) typeUtils.asElement(mirror))
          .filter(NOT_IGNORED)
          .collect(toList());
    }
    throw new RuntimeException("Cannot find type elements");
  }

  //Either we find a proper way of type checking validators and mappers or we leave it as is
  @Deprecated
  private Optional<TypeElement> validated(TypeMirror mirror, TypeName expectedType, Class<?> expectedBaseType,
                                          String parameterName,
                                          Element enclosingElement) {
    TypeElement typeElement = (TypeElement) typeUtils.asElement(mirror);
    if (IGNORED_TYPES.contains(typeElement.getQualifiedName().toString())) {
      return Optional.empty();
    }
    if (!isValid(typeElement, expectedType, expectedBaseType)) {
      throw new AnnotationProcessingException(
          String.format("Annotation parameter '%s' needs to be a '%s<%s>'",
              parameterName,
              expectedBaseType.getSimpleName(),
              expectedType),
          enclosingElement
      );
    }
    return Optional.of(typeElement);
  }

  private boolean isValid(TypeElement actual, TypeName typeParamName, Class<?> baseType) {
    return !actual.getInterfaces().stream()
        .map(TypeMirror::toString)
        .filter(matches(baseType.getCanonicalName() + "<" + typeParamName.toString() + ">"))
        .collect(toList()).isEmpty();

  }

  private Predicate<String> matches(String regex) {
    return value -> value.matches(regex);
  }
}
