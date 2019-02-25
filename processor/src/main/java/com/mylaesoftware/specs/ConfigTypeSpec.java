package com.mylaesoftware.specs;

import com.mylaesoftware.validators.ConfigValidationException;
import com.mylaesoftware.validators.ValidationError;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.typesafe.config.Config;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

public class ConfigTypeSpec {

  private static final ConfigTypeSpec EMPTY = new ConfigTypeSpec();

  public static final String CLASS_NAME = "GlobalConfig";

  public static final BinaryOperator<TypeSpec.Builder> specMerger = (left, right) -> {
    TypeSpec one = left.build();
    TypeSpec other = right.build();
    return TypeSpec.classBuilder(CLASS_NAME)
        .addModifiers(one.modifiers.toArray(new Modifier[0]))
        .addSuperinterfaces(one.superinterfaces)
        .addFields(ConfigTypeSpecReducer.append(one.fieldSpecs, other.fieldSpecs))
        .addMethods(ConfigTypeSpecReducer.append(one.methodSpecs, other.methodSpecs));
  };

  final String packageName;
  final Set<TypeMirror> superInterfaces;
  final Map<ClassName, Collection<ConfigValueSpec>> configValues;
  final Map<ClassName, Collection<ClassName>> validators;

  private ConfigTypeSpec() {
    packageName = "";
    superInterfaces = emptySet();
    configValues = emptyMap();
    validators = emptyMap();
  }

  public ConfigTypeSpec(String packageName,
                        Set<TypeMirror> superInterfaces,
                        Map<ClassName, Collection<ConfigValueSpec>> configValues,
                        Map<ClassName, Collection<ClassName>> validators) {
    this.packageName = packageName;
    this.superInterfaces = superInterfaces;
    this.configValues = configValues;
    this.validators = validators;
  }

  public String packageName() {
    return packageName;
  }

  public TypeSpec build() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(ConfigTypeSpec.CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterfaces(superInterfaces.stream().map(TypeName::get).collect(toList()));

    //TODO: Parallelize this stream by copying builder at each step, rather then modifying base builder
    return configValues.values().stream().flatMap(Collection::stream).reduce(builder,
        (b, configValue) -> b.addField(configValue.getField())
            .addMethod(configValue.getInitMethod())
            .addMethod(configValue.getOverrideMethod()),
        specMerger)
        .addMethod(buildConstructor())
        .addMethod(buildValidationMethod())
        .build();
  }

  private MethodSpec buildConstructor() {
    final MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Config.class, "config", Modifier.FINAL);

    return configValues.values().stream().flatMap(Collection::stream).collect(() -> builder,
        (b, configValue) ->
            b.addStatement(configValue.getField().name + " = " + configValue.getInitMethod().name + "($N)", "config"),
        (l, r) -> l.addCode(r.build().code)
    )
        .addStatement("$T<$T> errors = validate()", List.class, ValidationError.class)
        .beginControlFlow("if (!errors.isEmpty())")
        .addStatement("throw new $T(errors)", ConfigValidationException.class)
        .endControlFlow()
        .build();
  }

  private MethodSpec buildValidationMethod() {
    return MethodSpec.methodBuilder("validate")
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .returns(List.class)
        .addStatement("$T<$T> errors = new $T<>()", List.class, ValidationError.class, ArrayList.class)
        .addCode(configValuesValidationCode())
        .addCode(configTypesValidationCode())
        .addStatement("return errors")
        .build();
  }

  private CodeBlock configTypesValidationCode() {
    return validators.entrySet().stream()
        .map(this::toValidationCode)
        .reduce(CodeBlock.builder(), CodeBlock.Builder::add, (l, r) -> l.add(r.build()))
        .build();
  }

  private CodeBlock configValuesValidationCode() {
    return configValues.entrySet().stream()
        .map(this::toValuesValidationCode)
        .reduce(CodeBlock.builder(), CodeBlock.Builder::add, (l, r) -> l.add(r.build()))
        .build();
  }

  private CodeBlock toValuesValidationCode(Map.Entry<ClassName, Collection<ConfigValueSpec>> entry) {

    return entry.getValue().stream().flatMap(configValue ->
        configValue.getValidators().stream()
            .map(validator ->
                CodeBlock.builder().add("errors.addAll(\n")
                    .add("new $T().apply($L).stream()\n", validator, configValue.getField().name)
                    .add(".map(e -> e.withFieldInfo($T.class, \"$L\"))\n", entry.getKey(), configValue.getField().name)
                    .add(".collect($T.toList())\n", Collectors.class)
                    .addStatement(")")

            )
    ).reduce((l, r) -> l.add(r.build())).orElse(CodeBlock.builder()).build();
  }

  private CodeBlock toValidationCode(Map.Entry<ClassName, Collection<ClassName>> entry) {

    return entry.getValue().stream()
        .collect(CodeBlock::builder,
            (builder, validator) -> builder.add("errors.addAll(\n")
                .add("new $T().apply($L).stream()\n", validator, "this")
                .add(".map(e -> e.withClassInfo($T.class))\n", entry.getKey())
                .add(".collect($T.toList())\n", Collectors.class)
                .addStatement(")"),
            (l, r) -> l.add(r.build())
        ).build();
  }

  public static ConfigTypeSpec empty() {
    return EMPTY;
  }

  public boolean isEmpty() {
    return this.equals(EMPTY);
  }

}
