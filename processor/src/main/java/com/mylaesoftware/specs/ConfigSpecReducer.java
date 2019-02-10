package com.mylaesoftware.specs;

import com.mylaesoftware.Config;
import com.mylaesoftware.ConfigValue;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.getCommonPrefix;

public class ConfigSpecReducer {

  public static final BiFunction<ConfigSpec, TypeElement, ConfigSpec> accumulator = (acc, element) -> {
    if (!element.getKind().isInterface()) {
      throw new RuntimeException("@Config can only be used on interfaces");
    }

    String rootKey = Optional.ofNullable(element.getAnnotation(Config.class)).map(Config::rootKey).orElse("");
    String packageName = ClassName.get(element).packageName();

    Set<TypeMirror> interfaces = new HashSet<>(singletonList(element.asType()));
    Set<ConfigValueSpec> configReaders = element.getEnclosedElements().stream()
        // non-annotated methods in config interface should have default impl otherwise fail.
        .filter(e -> ElementKind.METHOD.equals(e.getKind()) && null != e.getAnnotation(ConfigValue.class))
        .map(method -> new ConfigValueSpec(rootKey, (Symbol.MethodSymbol) method))
        .collect(toSet());
    return acc.isEmpty()
        ? new ConfigSpec(packageName, interfaces, configReaders)
        : new ConfigSpec(
        getCommonPrefix(packageName, acc.packageName),
        append(interfaces, acc.superInterfaces),
        append(configReaders, acc.configValues));
  };


  public static final BinaryOperator<ConfigSpec> combiner = (left, right) -> {
    if (left.isEmpty()) {
      return right;
    }
    if (right.isEmpty()) {
      return left;
    }

    String packageName = getCommonPrefix(left.packageName, right.packageName);

    return new ConfigSpec(packageName,
        append(left.superInterfaces, right.superInterfaces),
        append(left.configValues, right.configValues)
    );
  };

  public final static BinaryOperator<TypeSpec.Builder> specMerger = (left, right) -> {
    TypeSpec one = left.build();
    TypeSpec other = right.build();
    return TypeSpec.classBuilder(ConfigSpec.className)
        .addModifiers(one.modifiers.toArray(new Modifier[0]))
        .addSuperinterfaces(one.superinterfaces)
        .addFields(append(one.fieldSpecs, other.fieldSpecs))
        .addMethods(append(one.methodSpecs, other.methodSpecs));
  };

  public static <T> Set<T> append(Set<T> one, Set<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

  public static <T> Set<T> append(List<T> one, List<T> other) {
    return Stream.concat(one.stream(), other.stream()).collect(toSet());
  }

}
