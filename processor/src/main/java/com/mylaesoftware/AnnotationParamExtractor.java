package com.mylaesoftware;

import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.NoValidation;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private final Elements elementUtils;

  public AnnotationParamExtractor(Types typeUtils, Elements elementUtils) {
    this.typeUtils = typeUtils;
    this.elementUtils = elementUtils;
  }

  public Optional<TypeElement> extractElement(Supplier<Class<?>> annotationParam,
                                              TypeName expectedGenericType,
                                              Class<?> expectedBaseType,
                                              String parameterName,
                                              Element enclosingElement) {
    try {
      annotationParam.get();
    } catch (MirroredTypeException mte) {
      return validated(mte.getTypeMirror(), expectedGenericType, expectedBaseType, parameterName, enclosingElement);
    }
    throw new RuntimeException("Cannot find type element");
  }

  public List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam,
                                           TypeName expectedGenericType,
                                           Class<?> expectedBaseType,
                                           String parameterName,
                                           Element enclosingElement) {
    try {
      annotationParam.get();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors().stream()
          .flatMap(mirror ->
              validated(mirror, expectedGenericType, expectedBaseType, parameterName, enclosingElement)
                  .map(Stream::of)
                  .orElse(Stream.empty())
          )
          .collect(toList());
    }
    throw new RuntimeException("Cannot find type elements");
  }


  private Optional<TypeElement> validated(TypeMirror mirror, TypeName expectedType,
                                          Class<?> expectedBaseType,
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
    List<TypeName> innerTypeParams = typeParamName.getClass().equals(ParameterizedTypeName.class)
        ? ((ParameterizedTypeName) typeParamName).typeArguments
        : Collections.emptyList();
    return !actual.getInterfaces().stream()
//        .map(TypeMirror::toString)
        .map(extractGenericType(baseType.getCanonicalName() + "<(?<type>.*?)>", innerTypeParams))
        .filter(isValidType(typeParamName))
        .collect(toList())
        .isEmpty();

  }

  private Predicate<Optional<TypeMirror>> isValidType(TypeName typeName) {
    String type = typeName.getClass().equals(ParameterizedTypeName.class)
        ? ((ParameterizedTypeName) typeName).rawType.toString()
        : typeName.toString();

    TypeMirror expectedType = typeUtils.erasure(elementUtils.getTypeElement(type).asType());

    return maybeMirror -> maybeMirror.filter(mirror -> typeUtils.isAssignable(expectedType, mirror)).isPresent();
  }

  private Function<TypeMirror, Optional<TypeMirror>> extractGenericType(String regex, List<TypeName> innerTypeParams) {
    return value -> {
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(value.toString());

//      if (!innerTypeParams.isEmpty()) {
//        typeUtils.getDeclaredType(elementUtils.value, elementUtils.getTypeElement(innerTypeParams.get(0).toString()))
//      }
      return matcher.matches()
          ? Optional.of(elementUtils.getTypeElement(matcher.group("type").replaceAll("<.*?>", "")).asType())
          : Optional.empty();
    };
  }

  private boolean isAssignable(TypeMirror type, TypeMirror value) {
    return true;
  }

}
