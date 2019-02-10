package com.mylaesoftware.specs;

import com.mylaesoftware.ConfigValue;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.code.Symbol;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;

public class ConfigValueSpec {

  final Symbol.MethodSymbol abstractMethod;
  final ConfigValue configValueAnnotation;
  final FieldSpec field;
  final MethodSpec initMethod;
  final MethodSpec overrideMethod;

  public ConfigValueSpec(String rootKey, Symbol.MethodSymbol abstractMethod) {
    this.abstractMethod = abstractMethod;
    this.configValueAnnotation = abstractMethod.getAnnotation(ConfigValue.class);
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
        .addStatement("return config." + typesafeParserFor(field.type) + "(\"$L\")", configKey)
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

  private String typesafeParserFor(TypeName type) {
    if (type.equals(TypeName.BOOLEAN)) {
      return "getBoolean";
    }
    if (type.equals(TypeName.INT)) {
      return "getInt";
    }
    if (type.equals(TypeName.get(String.class))) {
      return "getString";
    }
    throw new RuntimeException("No Typesafe parsing method for type: " + type);
  }
}
