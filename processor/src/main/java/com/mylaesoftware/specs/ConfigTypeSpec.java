package com.mylaesoftware.specs;

import com.mylaesoftware.GlobalConfig;
import com.mylaesoftware.validators.ConfigValidationException;
import com.mylaesoftware.validators.ValidationError;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.mylaesoftware.specs.ConfigTypeSpecReducer.append;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class ConfigTypeSpec {

  private static final ConfigTypeSpec EMPTY = new ConfigTypeSpec();

  private static final Collector<CodeBlock, ?, CodeBlock> TO_CODE_BLOCK = CodeBlock.joining("");
  private static final String VALIDATION_METHOD_NAME = "validate";

  final Set<TypeMirror> superInterfaces;
  final Map<ClassName, Collection<ConfigValueSpec>> configValues;
  final Map<ClassName, Collection<ClassName>> validators;

  private ConfigTypeSpec() {
    superInterfaces = emptySet();
    configValues = emptyMap();
    validators = emptyMap();
  }

  public ConfigTypeSpec(Set<TypeMirror> superInterfaces,
                        Map<ClassName, Collection<ConfigValueSpec>> configValues,
                        Map<ClassName, Collection<ClassName>> validators) {
    this.superInterfaces = superInterfaces;
    this.configValues = configValues;
    this.validators = validators;
  }

  public String packageName() {
    return GlobalConfig.class.getPackage().getName();
  }

  public TypeSpec build() {

    TypeSpec spec = TypeSpec.classBuilder(GlobalConfig.IMPLEMENTATION_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterfaces(superInterfaces.parallelStream().map(TypeName::get).collect(toSet()))
        .addSuperinterface(ClassName.get(GlobalConfig.class))
        .addMethod(buildConstructor())
        .addMethod(buildValidationMethod())
        .build();

    return configValues.values().stream().flatMap(Collection::stream).parallel()
        .reduce(spec, ConfigTypeSpec::accumulate, ConfigTypeSpec::combine);

  }

  private MethodSpec buildConstructor() {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Config.class, "config", Modifier.FINAL)
        .addCode(fieldsAssignmentCode())
        .addStatement("$T<$T> errors = $L()", List.class, ValidationError.class, VALIDATION_METHOD_NAME)
        .beginControlFlow("if (!errors.isEmpty())")
        .addStatement("throw new $T(errors)", ConfigValidationException.class)
        .endControlFlow()
        .build();
  }

  private CodeBlock fieldsAssignmentCode() {
    return configValues.values().stream().flatMap(Collection::stream).parallel()
        .map(value ->
            CodeBlock.builder()
                .addStatement(value.getField().name + " = " + value.getInitMethod().name + "($N)", "config")
                .build())
        .collect(TO_CODE_BLOCK);
  }

  private MethodSpec buildValidationMethod() {
    return MethodSpec.methodBuilder(VALIDATION_METHOD_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .returns(ParameterizedTypeName.get(List.class, ValidationError.class))
        .addStatement("$T<$T> errors = new $T<>()", List.class, ValidationError.class, ArrayList.class)
        .addCode(typesValidationCode())
        .addCode(fieldsValidationCode())
        .addStatement("return errors")
        .build();
  }

  private CodeBlock fieldsValidationCode() {
    return validators.entrySet().parallelStream()
        .map(this::toTypesValidationCode)
        .collect(CodeBlock.joining(""));
  }

  private CodeBlock typesValidationCode() {
    return configValues.entrySet().parallelStream()
        .map(this::toFieldsValidationCode)
        .collect(TO_CODE_BLOCK);
  }

  private CodeBlock toFieldsValidationCode(Map.Entry<ClassName, Collection<ConfigValueSpec>> entry) {

    return entry.getValue().parallelStream().flatMap(configValue ->
        configValue.getValidators().parallelStream()
            .map(validator ->
                CodeBlock.builder().add("errors.addAll(\n")
                    .add("new $T().apply($L).stream()\n", validator, configValue.getField().name)
                    .add(".map(e -> e.withFieldInfo($T.class, \"$L\"))\n", entry.getKey(), configValue.getField().name)
                    .add(".collect($T.toList())\n", Collectors.class)
                    .addStatement(")")
                    .build()

            )
    ).collect(TO_CODE_BLOCK);
  }

  private CodeBlock toTypesValidationCode(Map.Entry<ClassName, Collection<ClassName>> entry) {

    return entry.getValue().parallelStream().map(validator ->
        CodeBlock.builder().add("errors.addAll(\n")
            .add("new $T().apply($L).stream()\n", validator, "this")
            .add(".map(e -> e.withClassInfo($T.class))\n", entry.getKey())
            .add(".collect($T.toList())\n", Collectors.class)
            .addStatement(")")
            .build()
    ).collect(TO_CODE_BLOCK);
  }

  private static TypeSpec combine(TypeSpec one, TypeSpec other) {
    return TypeSpec.classBuilder(GlobalConfig.IMPLEMENTATION_NAME)
        .addModifiers(append(one.modifiers, other.modifiers).toArray(new Modifier[0]))
        .addSuperinterfaces(append(one.superinterfaces, other.superinterfaces))
        .addFields(append(one.fieldSpecs, other.fieldSpecs))
        .addMethods(append(one.methodSpecs, other.methodSpecs)).build();
  }

  private static TypeSpec accumulate(TypeSpec accumulated, ConfigValueSpec value) {
    return accumulated.toBuilder().addField(value.getField())
        .addMethod(value.getInitMethod())
        .addMethod(value.getOverrideMethod())
        .build();
  }

  public static ConfigTypeSpec empty() {
    return EMPTY;
  }

  public boolean isEmpty() {
    return this.equals(EMPTY);
  }

}
