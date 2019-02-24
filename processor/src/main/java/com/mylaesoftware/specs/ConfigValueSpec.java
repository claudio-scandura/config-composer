package com.mylaesoftware.specs;

import com.mylaesoftware.Annotations;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.BasicMappers.StringM;
import com.mylaesoftware.mappers.NoMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Types;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.mylaesoftware.mappers.BasicMappers.AnyRefM;
import static com.mylaesoftware.mappers.BasicMappers.BooleanM;
import static com.mylaesoftware.mappers.BasicMappers.ConfigM;
import static com.mylaesoftware.mappers.BasicMappers.ConfigValueM;
import static com.mylaesoftware.mappers.BasicMappers.DoubleM;
import static com.mylaesoftware.mappers.BasicMappers.DurationM;
import static com.mylaesoftware.mappers.BasicMappers.IntM;
import static com.mylaesoftware.mappers.BasicMappers.LongM;
import static com.mylaesoftware.mappers.BasicMappers.NumberM;
import static com.mylaesoftware.mappers.CollectionsMappers.AnyRefListM;
import static com.mylaesoftware.mappers.CollectionsMappers.BooleanListM;
import static com.mylaesoftware.mappers.CollectionsMappers.ConfigListM;
import static com.mylaesoftware.mappers.CollectionsMappers.DoubleListM;
import static com.mylaesoftware.mappers.CollectionsMappers.DurationListM;
import static com.mylaesoftware.mappers.CollectionsMappers.IntListM;
import static com.mylaesoftware.mappers.CollectionsMappers.LongListM;
import static com.mylaesoftware.mappers.CollectionsMappers.NumberListM;
import static com.mylaesoftware.mappers.CollectionsMappers.StringListM;
import static com.sun.tools.javac.code.Symbol.MethodSymbol;

public class ConfigValueSpec {

  private final Types typeUtils;
  final MethodSymbol abstractMethod;
  final ConfigValue configValueAnnotation;
  final FieldSpec field;
  final MethodSpec initMethod;
  final MethodSpec overrideMethod;

  public ConfigValueSpec(String contextPath, ConfigValue annotation, MethodSymbol abstractMethod, Types typeUtils) {
    this.typeUtils = typeUtils;
    this.abstractMethod = abstractMethod;
    this.configValueAnnotation = annotation;
    String configPath = contextPath.isEmpty()
        ? configValueAnnotation.atPath()
        : contextPath.concat("." + configValueAnnotation.atPath());

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
        .addCode(buildInitStatement(configPath, isOptionalField()))
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

  private boolean isOptionalField() {
    return ParameterizedTypeName.class.equals(field.type.getClass())
        && ((ParameterizedTypeName) field.type).rawType.equals(ClassName.get(Optional.class));
  }

  private CodeBlock buildInitStatement(String configPath, boolean isOptional) {
    TypeName typeName = isOptional ? ((ParameterizedTypeName) field.type).typeArguments.get(0) : field.type;

    ClassName mapper = customMapper().orElseGet(() -> mapperFor(typeName));
    String returnStmt = "new $T().apply(config, \"$L\")";
    CodeBlock.Builder builder = CodeBlock.builder();

    if (isOptional) {
      return builder.beginControlFlow("try")
          .addStatement("return Optional.of(" + returnStmt + ")", mapper, configPath)
          .nextControlFlow("catch ($T e)", ConfigException.class)
          .addStatement("return Optional.empty()")
          .endControlFlow()
          .build();
    }
    return builder.addStatement("return " + returnStmt, mapper, configPath).build();
  }

  private Optional<ClassName> customMapper() {
    try {
      configValueAnnotation.mappedBy();
    } catch (MirroredTypeException mte) {
      return Optional.of((TypeElement) typeUtils.asElement(mte.getTypeMirror()))
          .filter(te -> !te.getQualifiedName().toString().equals(NoMapper.class.getCanonicalName()))
          .map(ClassName::get);
    }
    throw new RuntimeException("Cannot find type element for mappedBy field in " + Annotations.CONFIG_VALUE.name);
  }


  private ClassName mapperFor(TypeName type) {
    type = type.isBoxedPrimitive() ? type.unbox() : type;
    if (type.toString().equals(Config.class.getCanonicalName())) {
      return ClassName.get(ConfigM.class);
    }
    if (type.toString().equals(com.typesafe.config.ConfigValue.class.getCanonicalName())) {
      return ClassName.get(ConfigValueM.class);
    }
    if (type.toString().equals(Duration.class.getCanonicalName())) {
      return ClassName.get(DurationM.class);
    }
    if (type.equals(TypeName.BOOLEAN)) {
      return ClassName.get(BooleanM.class);
    }
    if (type.equals(TypeName.INT)) {
      return ClassName.get(IntM.class);
    }
    if (type.equals(TypeName.LONG)) {
      return ClassName.get(LongM.class);
    }
    if (type.toString().equals(Number.class.getCanonicalName())) {
      return ClassName.get(NumberM.class);
    }
    if (type.equals(TypeName.DOUBLE)) {
      return ClassName.get(DoubleM.class);
    }
    if (type.equals(TypeName.get(String.class))) {
      return ClassName.get(StringM.class);
    }
    if (type.equals(TypeName.OBJECT)) {
      return ClassName.get(AnyRefM.class);
    }

    return collectionMapperFor(type);
  }

  private ClassName collectionMapperFor(TypeName type) {
    if (type.equals(ParameterizedTypeName.get(List.class, Config.class))) {
      return ClassName.get(ConfigListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Duration.class))) {
      return ClassName.get(DurationListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Boolean.class))) {
      return ClassName.get(BooleanListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Integer.class))) {
      return ClassName.get(IntListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Long.class))) {
      return ClassName.get(LongListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Number.class))) {
      return ClassName.get(NumberListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Double.class))) {
      return ClassName.get(DoubleListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, String.class))) {
      return ClassName.get(StringListM.class);
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Object.class))) {
      return ClassName.get(AnyRefListM.class);
    }

    throw new AnnotationProcessingException(
        String.format("Unsupported config value type '%s'. Please provide a custom mapper", type),
        abstractMethod
    );
  }

}
