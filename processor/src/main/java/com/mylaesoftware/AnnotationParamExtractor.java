package com.mylaesoftware;

import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.NoValidation;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class AnnotationParamExtractor {

  private static final Set<String> IGNORED_TYPES = Stream.of(NoMapper.class, NoValidation.class)
      .map(Class::getCanonicalName)
      .collect(toSet());

  private final Types typeUtils;
  private final Elements elementUtils;

  public AnnotationParamExtractor(Types typeUtils, Elements elementUtils) {
    this.typeUtils = typeUtils;
    this.elementUtils = elementUtils;
  }

  public Optional<TypeElement> extractElement(Supplier<Class<?>> annotationParam,
                                              ParameterizedTypeName expectedGenericType,
                                              String parameterName,
                                              Element enclosingElement) {
    try {
      annotationParam.get();
    } catch (MirroredTypeException mte) {
      return validated(mte.getTypeMirror(), expectedGenericType, parameterName, enclosingElement);
    }
    throw new RuntimeException("Cannot find type element");
  }

  public List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam,
                                           ParameterizedTypeName expectedGenericType,
                                           String parameterName,
                                           Element enclosingElement) {
    try {
      annotationParam.get();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors().stream()
          .flatMap(mirror ->
              validated(mirror, expectedGenericType, parameterName, enclosingElement)
                  .map(Stream::of)
                  .orElse(Stream.empty())
          )
          .collect(toList());
    }
    throw new RuntimeException("Cannot find type elements");
  }


  private Optional<TypeElement> validated(TypeMirror mirror,
                                          ParameterizedTypeName expectedType,
                                          String parameterName,
                                          Element enclosingElement) {
    TypeElement typeElement = (TypeElement) typeUtils.asElement(mirror);
    if (IGNORED_TYPES.contains(typeElement.getQualifiedName().toString())) {
      return Optional.empty();
    }
    if (!isValid(typeElement, expectedType)) {
      throw new AnnotationProcessingException(
          String.format("Annotation parameter '%s' needs to be a '%s'",
              parameterName,
              expectedType),
          enclosingElement
      );
    }
    return Optional.of(typeElement);
  }

  private boolean isValid(TypeElement actual, ParameterizedTypeName expectedType) {
    return !actual.getInterfaces().stream()
        .map(TypeName::get)
        .filter(ParameterizedTypeName.class::isInstance)
        .filter(isAssignableTo(expectedType))
        .collect(toList())
        .isEmpty();

  }

  private Predicate<TypeName> isAssignableTo(TypeName expectedType) {
    return actualType -> {
      TypeMirror expectedErasedType = typeUtils.erasure(asTypeMirror(expectedType));
      TypeMirror actualErasedValue = typeUtils.erasure(asTypeMirror(actualType));

      if (!typeUtils.isAssignable(expectedErasedType, actualErasedValue)) {
        return false;
      }
      if (expectedType.getClass().equals(ParameterizedTypeName.class)) {
        if (actualType.getClass().equals(ParameterizedTypeName.class)) {

          List<TypeName> expectedTypeArgs = ((ParameterizedTypeName) expectedType).typeArguments;
          List<TypeName> actualTypeArgs = ((ParameterizedTypeName) actualType).typeArguments;
          return IntStream.range(0, Math.min(expectedTypeArgs.size(), actualTypeArgs.size())).allMatch(idx ->
              isAssignableTo(expectedTypeArgs.get(idx)).test(actualTypeArgs.get(idx))
          );
        }
        return false;
      }
      return true;
    };
  }

  private TypeMirror asTypeMirror(TypeName name) {
    if (name.getClass().equals(WildcardTypeName.class)) {
      final WildcardTypeName wildCard = (WildcardTypeName) name;
      TypeMirror lowerBound = wildCard.lowerBounds.isEmpty()
          ? null
          : asTypeMirror(wildCard.lowerBounds.get(0));
      TypeMirror upperBound = wildCard.upperBounds.isEmpty()
          ? null
          : asTypeMirror(wildCard.upperBounds.get(0));
      return typeUtils.getWildcardType(upperBound, lowerBound);
    }
    return elementUtils.getTypeElement(name.getClass().equals(ParameterizedTypeName.class)
        ? ((ParameterizedTypeName) name).rawType.toString()
        : name.toString()).asType();
  }
}
