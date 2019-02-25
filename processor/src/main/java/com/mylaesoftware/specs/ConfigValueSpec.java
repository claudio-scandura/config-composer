package com.mylaesoftware.specs;

import com.mylaesoftware.MirroredTypesExtractor;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.BasicMappers.StringM;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.NoValidation;
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

import java.time.Duration;
import java.util.Collection;
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
import static java.util.stream.Collectors.toSet;

public class ConfigValueSpec {

  private final MirroredTypesExtractor typesExtractor;
  private final MethodSymbol abstractMethod;
  private final ConfigValue configValueAnnotation;
  private final FieldSpec field;
  private final MethodSpec initMethod;
  private final MethodSpec overrideMethod;
  private final Collection<ClassName> validators;

  public ConfigValueSpec(String contextPath, ConfigValue annotation,
                         MethodSymbol abstractMethod,
                         MirroredTypesExtractor typesExtractor) {

    this.typesExtractor = typesExtractor;
    this.abstractMethod = abstractMethod;
    this.configValueAnnotation = annotation;

    String configPath = contextPath.isEmpty()
        ? configValueAnnotation.atPath()
        : contextPath.concat("." + configValueAnnotation.atPath());

    validators = validators();
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

  public Collection<ClassName> getValidators() {
    return validators;
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
          .addStatement("return Optional.ofNullable(" + returnStmt + ")", mapper, configPath)
          .nextControlFlow("catch ($T e)", ConfigException.Missing.class)
          .addStatement("return Optional.empty()")
          .endControlFlow()
          .build();
    }
    return builder.addStatement("return " + returnStmt, mapper, configPath).build();
  }

  private Optional<ClassName> customMapper() {
    return Optional.of(typesExtractor.extractType(configValueAnnotation::mappedBy))
        .filter(te -> !te.getQualifiedName().toString().equals(NoMapper.class.getCanonicalName()))
        //TODO: Add validation of type parameter (needs to match annotated field type)
        .map(ClassName::get);
  }


  private Collection<ClassName> validators() {
    return typesExtractor.extractTypes(configValueAnnotation::validatedBy).stream()
        .filter(te -> !te.getQualifiedName().toString().equals(NoValidation.class.getCanonicalName()))
        .map(ClassName::get).collect(toSet());
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
