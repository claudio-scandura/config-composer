package com.mylaesoftware.specs;

import com.mylaesoftware.Annotations;
import com.mylaesoftware.NoMapper;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.code.Symbol;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Types;

import java.util.Optional;

public class ConfigValueSpec {

  private final Types typeUtils;
  final Symbol.MethodSymbol abstractMethod;
  final ConfigValue configValueAnnotation;
  final FieldSpec field;
  final MethodSpec initMethod;
  final MethodSpec overrideMethod;

  public ConfigValueSpec(String rootKey, ConfigValue annotation, Symbol.MethodSymbol abstractMethod, Types typeUtils) {
    this.typeUtils = typeUtils;
    this.abstractMethod = abstractMethod;
    this.configValueAnnotation = annotation;
    String configKey = rootKey.isEmpty()
        ? configValueAnnotation.atKey()
        : rootKey.concat("." + configValueAnnotation.atKey());

    String methodName = abstractMethod.getSimpleName().toString();
    field = FieldSpec.builder(
        TypeName.get(abstractMethod.getReturnType()),
        methodName,
        Modifier.PRIVATE,
        Modifier.FINAL
    ).build();

    initMethod = MethodSpec.methodBuilder("read" + StringUtils.capitalize(methodName))
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .addParameter(Config.class, "config", Modifier.FINAL)
        .returns(field.type)
        .addStatement(buildInitStatement(), configKey)
        .build();

    overrideMethod = MethodSpec.methodBuilder(methodName)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(field.type)
        .addStatement("return $L", field.name)
        .build();
  }

  public FieldSpec getField() {
    return field;
  }

  public MethodSpec getInitMethod() {
    return initMethod;
  }

  public MethodSpec getOverrideMethod() {
    return overrideMethod;
  }

  private String buildInitStatement() {
    return customMapperType()
        .map(TypeElement::getQualifiedName)
        .map(name -> String.format("return new %s().apply(config, \"$L\")", name))
        .orElseGet(() -> "return config." + mapperFor(field.type) + "(\"$L\")");
  }

  private String mapperFor(TypeName type) {
    if (type.equals(TypeName.BOOLEAN)) {
      return "getBoolean";
    }
    if (type.equals(TypeName.INT)) {
      return "getInt";
    }
    if (type.equals(TypeName.get(String.class))) {
      return "getString";
    }
    throw new AnnotationProcessingException(
        String.format("Unsupported config value type '%s'. Please provide a custom mapper", type),
        abstractMethod
    );
  }

  private Optional<TypeElement> customMapperType() {
    try {
      configValueAnnotation.mapper();
    } catch (MirroredTypeException mte) {
      return Optional.of((TypeElement) typeUtils.asElement(mte.getTypeMirror()))
          .filter(te -> !te.getQualifiedName().toString().equals(NoMapper.class.getCanonicalName()));
    }
    throw new RuntimeException("Cannot find type element for mapper field in " + Annotations.CONFIG_VALUE.name);
  }

}
