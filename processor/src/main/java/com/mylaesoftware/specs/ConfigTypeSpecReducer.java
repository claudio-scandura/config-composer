package com.mylaesoftware.specs;

import com.mylaesoftware.AnnotationParamExtractor;
import com.mylaesoftware.Annotations;
import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.validators.ConfigValidator;
import com.squareup.javapoet.ClassName;

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
import static org.apache.commons.lang3.StringUtils.getCommonPrefix;

public class ConfigTypeSpecReducer {

  private final AnnotationParamExtractor typesExtractor;

  public ConfigTypeSpecReducer(AnnotationParamExtractor typesExtractor) {
    this.typesExtractor = typesExtractor;
  }

  public ConfigTypeSpec accumulate(ConfigTypeSpec spec, TypeElement element) {
    if (!element.getKind().isInterface()) {
      throw new AnnotationProcessingException(
          Annotations.CONFIG_TYPE.name + " annotation can only be used on interfaces",
          element
      );
    }

    ConfigType annotation = element.getAnnotation(ConfigType.class);
    String contextPath = annotation.contextPath();

    ClassName interfaceName = ClassName.get(element);
    Map<ClassName, Collection<ClassName>> validators = singletonMap(
        interfaceName,
        validators(annotation, element)
    );

    Set<TypeMirror> interfaces = new HashSet<>(singletonList(element.asType()));
    Map<ClassName, Collection<ConfigValueSpec>> configValues = singletonMap(
        interfaceName,
        element.getEnclosedElements().stream()
            .filter(e -> ElementKind.METHOD.equals(e.getKind()))
            .flatMap(toConfigValue(contextPath))
            .collect(toSet())
    );

    return spec.isEmpty()
        ? new ConfigTypeSpec(interfaceName.packageName(), interfaces, configValues, validators) :
        new ConfigTypeSpec(
            getCommonPrefix(interfaceName.packageName(), spec.packageName),
            append(interfaces, spec.superInterfaces),
            append(configValues, spec.configValues),
            append(validators, spec.validators)
        );
  }

  public ConfigTypeSpec combine(ConfigTypeSpec one, ConfigTypeSpec other) {
    if (one.isEmpty()) {
      return other;
    }
    if (other.isEmpty()) {
      return one;
    }

    String packageName = getCommonPrefix(one.packageName, other.packageName);

    return new ConfigTypeSpec(packageName,
        append(one.superInterfaces, other.superInterfaces),
        append(one.configValues, other.configValues),
        append(one.validators, other.validators)
    );
  }

  private Collection<ClassName> validators(ConfigType type, TypeElement element) {
    return typesExtractor.extractElementsWithValidType(
        type::validatedBy,
        "validatedBy",
        ClassName.get(element),
        element, ConfigValidator.class
    ).stream()
        .map(ClassName::get)
        .collect(toSet());
  }

  public static <T> Set<T> append(Set<T> one, Set<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

  public static <T> Set<T> append(List<T> one, List<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

  public static <K, V> Map<K, V> append(Map<K, V> one, Map<K, V> other) {
    return Stream.concat(one.entrySet().stream(), other.entrySet().stream())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Function<Element, Stream<ConfigValueSpec>> toConfigValue(String rootKey) {
    return method -> {
      if (method.getModifiers().contains(Modifier.DEFAULT)) {
        return Stream.empty();
      }
      return Optional.ofNullable(method.getAnnotation(ConfigValue.class))
          .map(annotation -> Stream.of(new ConfigValueSpec(rootKey, annotation, (MethodSymbol) method, typesExtractor)))
          .orElseThrow(() ->
              new AnnotationProcessingException("Abstract method needs to be annotated with " + CONFIG_VALUE.name +
                  " or have default implementation", method)
          );
    };
  }

}
