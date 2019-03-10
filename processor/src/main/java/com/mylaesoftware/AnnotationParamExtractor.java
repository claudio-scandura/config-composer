package com.mylaesoftware;

import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.NoValidation;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
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
                                              Class<?> outerType,
                                              String parameterName,
                                              MethodSymbol annotatedMethod) {
    try {
      annotationParam.get();
    } catch (MirroredTypeException mte) {
      DeclaredType declaredType = declaredTypeFor(outerType, annotatedMethod.getReturnType());
      return validated(mte.getTypeMirror(), declaredType, parameterName, annotatedMethod);
    }
    throw new RuntimeException("Cannot find type element");
  }

  public List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam,
                                           Class<?> outerType,
                                           String parameterName,
                                           MethodSymbol annotatedMethod) {
    return extractElements(annotationParam, outerType, parameterName, annotatedMethod.getReturnType(), annotatedMethod);
  }

  public List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam,
                                           Class<?> outerType,
                                           String parameterName,
                                           TypeElement annotatedType) {
    return extractElements(annotationParam, outerType, parameterName, annotatedType.asType(), annotatedType);
  }

  private List<TypeElement> extractElements(Supplier<Class<?>[]> annotationParam,
                                            Class<?> outerType,
                                            String parameterName,
                                            TypeMirror type,
                                            Element element) {
    try {
      annotationParam.get();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors().stream()
          .flatMap(mirror ->
              validated(mirror, declaredTypeFor(outerType, type), parameterName, element)
                  .map(Stream::of)
                  .orElse(Stream.empty())
          )
          .collect(toList());
    }
    throw new RuntimeException("Cannot find type elements");
  }

  private DeclaredType declaredTypeFor(Class<?> clazz, TypeMirror typeArg) {
    TypeElement te = elementUtils.getTypeElement(clazz.getName());
    return typeUtils.getDeclaredType(te, boxed(typeArg));
  }

  private TypeMirror boxed(TypeMirror type) {
    return (type instanceof Type && ((Type) type).isPrimitive())
        ? typeUtils.boxedClass((PrimitiveType) type).asType()
        : type;
  }

  private Optional<TypeElement> validated(TypeMirror mirror,
                                          DeclaredType expectedType,
                                          String parameterName,
                                          Element enclosingElement) {
    TypeElement typeElement = (TypeElement) typeUtils.asElement(mirror);
    if (IGNORED_TYPES.contains(typeElement.getQualifiedName().toString())) {
      return Optional.empty();
    }
    if (!isValid(typeElement, expectedType)) {
      throw new AnnotationProcessingException(
          String.format("Annotation parameter '%s' with type '%s' needs to be a subtype of '%s'",
              parameterName,
              typeElement,
              expectedType),
          enclosingElement
      );
    }
    return Optional.of(typeElement);
  }

  private boolean isValid(TypeElement actual, DeclaredType expectedType) {
    return !actual.getInterfaces().stream()
        .filter(DeclaredType.class::isInstance)
        .filter(isAssignableTo(expectedType))
        .collect(toList())
        .isEmpty();

  }

  private Predicate<TypeMirror> isAssignableTo(TypeMirror expectedType) {
    return actualType -> {
      TypeMirror expectedErasedType = typeUtils.erasure(expectedType);
      TypeMirror actualErasedValue = typeUtils.erasure(actualType);

      if (!typeUtils.isAssignable(expectedErasedType, actualErasedValue)) {
        return false;
      }

      if (actualType instanceof Type.WildcardType) {
        return true;
      }

      if (expectedType instanceof DeclaredType) {
        if (actualType instanceof DeclaredType) {

          List<? extends TypeMirror> expectedTypeArgs = ((DeclaredType) expectedType).getTypeArguments();
          List<? extends TypeMirror> actualTypeArgs = ((DeclaredType) actualType).getTypeArguments();
          return IntStream.range(0, Math.min(expectedTypeArgs.size(), actualTypeArgs.size())).allMatch(idx ->
              isAssignableTo(expectedTypeArgs.get(idx)).test(actualTypeArgs.get(idx))
          );
        }
        return false;
      }
      return true;
    };
  }

}
