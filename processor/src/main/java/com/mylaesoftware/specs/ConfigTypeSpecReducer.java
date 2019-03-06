package com.mylaesoftware.specs;

import com.mylaesoftware.AnnotationParamExtractor;
import com.mylaesoftware.Annotations;
import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.validators.ConfigValidator;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.mylaesoftware.Annotations.CONFIG_VALUE;
import static com.sun.tools.javac.code.Symbol.MethodSymbol;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class ConfigTypeSpecReducer {

  private final AnnotationParamExtractor typesExtractor;

  public ConfigTypeSpecReducer(AnnotationParamExtractor typesExtractor) {
    this.typesExtractor = typesExtractor;
  }

  public ConfigTypeSpec accumulate(ConfigTypeSpec accumulated, TypeElement element) {

    throwIfFalse(element.getKind().isInterface(), "annotation can only be used on interfaces", element);

    throwIfFalse(element.getTypeParameters().isEmpty(),
        "generics are not supported on ConfigType", element);

    throwIfFalse(element.getModifiers().contains(Modifier.PUBLIC),
        "annotation cannot be used on non public interfaces", element);

    ConfigType annotation = element.getAnnotation(ConfigType.class);

    ClassName interfaceName = ClassName.get(element);
    Map<ClassName, Collection<ClassName>> validators = singletonMap(interfaceName, validators(annotation, element));

    Set<TypeMirror> interfaces = new HashSet<>(singletonList(element.asType()));
    Map<ClassName, Collection<ConfigValueSpec>> configValues = singletonMap(
        interfaceName,
        element.getEnclosedElements().stream()
            .filter(e -> ElementKind.METHOD.equals(e.getKind()))
            .flatMap(toConfigValue(annotation))
            .collect(toSet())
    );

    ConfigTypeSpec spec = new ConfigTypeSpec(
        merge(interfaces, accumulated.superInterfaces),
        merge(configValues, accumulated.configValues),
        merge(validators, accumulated.validators)
    );

    return combine(accumulated, spec);
  }

  public ConfigTypeSpec combine(ConfigTypeSpec one, ConfigTypeSpec other) {
    return new ConfigTypeSpec(
        merge(one.superInterfaces, other.superInterfaces),
        merge(one.configValues, other.configValues),
        merge(one.validators, other.validators)
    );
  }

  private Collection<ClassName> validators(ConfigType type, TypeElement element) {
    return typesExtractor.extractElements(type::validatedBy,
        ParameterizedTypeName.get(ClassName.get(ConfigValidator.class), ClassName.get(element)),
        "validatedBy", element
    ).stream().map(ClassName::get).collect(toSet());
  }

  public static <T> Set<T> merge(Set<T> one, Set<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

  public static <T> Set<T> merge(List<T> one, List<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

  public static <K, V> Map<K, V> merge(Map<K, V> one, Map<K, V> other) {
    return Stream.concat(one.entrySet().stream(), other.entrySet().stream())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (l, r) -> r));
  }

  private Function<Element, Stream<ConfigValueSpec>> toConfigValue(ConfigType type) {
    return method -> {
      if (method.getModifiers().contains(Modifier.DEFAULT)) {
        return Stream.empty();
      }
      return Optional.ofNullable(method.getAnnotation(ConfigValue.class))
          .map(annotation -> Stream.of(new ConfigValueSpec(type.contextPath(), annotation, (MethodSymbol) method,
                  typesExtractor, type.fallbackToBeanMapper())
              )
          )
          .orElseThrow(() ->
              new AnnotationProcessingException("Abstract method needs to be annotated with " + CONFIG_VALUE.name +
                  " or have default implementation", method)
          );
    };
  }

  private static void throwIfFalse(boolean condition, String errorMessage, TypeElement element) {
    if (!condition) {
      throw new AnnotationProcessingException(
          Annotations.CONFIG_TYPE.name + " " + errorMessage,
          element
      );
    }
  }
}
