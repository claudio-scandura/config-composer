package com.mylaesoftware.specs;

import com.mylaesoftware.Annotations;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.mappers.BasicMappers.StringM;
import com.mylaesoftware.mappers.NoMapper;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.code.Symbol;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
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
        ? configValueAnnotation.atPath()
        : rootKey.concat("." + configValueAnnotation.atPath());

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
    String mapper = customMapper()
        .map(TypeElement::getQualifiedName)
        .map(Name::toString)
        .orElseGet(() -> basicMapperFor(field.type));
    return String.format("return new %s().apply(config, \"$L\")", mapper);
  }

  private Optional<TypeElement> customMapper() {
    try {
      configValueAnnotation.mapper();
    } catch (MirroredTypeException mte) {
      return Optional.of((TypeElement) typeUtils.asElement(mte.getTypeMirror()))
          .filter(te -> !te.getQualifiedName().toString().equals(NoMapper.class.getCanonicalName()));
    }
    throw new RuntimeException("Cannot find type element for mapper field in " + Annotations.CONFIG_VALUE.name);
  }


  private String basicMapperFor(TypeName type) {
    type = type.isBoxedPrimitive() ? type.unbox() : type;
    if (type.toString().equals(Config.class.getCanonicalName())) {
      return ConfigM.class.getCanonicalName();
    }
    if (type.toString().equals(com.typesafe.config.ConfigValue.class.getCanonicalName())) {
      return ConfigValueM.class.getCanonicalName();
    }
    if (type.toString().equals(Duration.class.getCanonicalName())) {
      return DurationM.class.getCanonicalName();
    }
    if (type.equals(TypeName.BOOLEAN)) {
      return BooleanM.class.getCanonicalName();
    }
    if (type.equals(TypeName.INT)) {
      return IntM.class.getCanonicalName();
    }
    if (type.equals(TypeName.LONG)) {
      return LongM.class.getCanonicalName();
    }
    if (type.toString().equals(Number.class.getCanonicalName())) {
      return NumberM.class.getCanonicalName();
    }
    if (type.equals(TypeName.DOUBLE)) {
      return DoubleM.class.getCanonicalName();
    }
    if (type.equals(TypeName.get(String.class))) {
      return StringM.class.getCanonicalName();
    }
    if (type.equals(TypeName.OBJECT)) {
      return AnyRefM.class.getCanonicalName();
    }

    return collectionMapperFor(type);
  }

  private String collectionMapperFor(TypeName type) {
    if (type.equals(ParameterizedTypeName.get(List.class, Config.class))) {
      return ConfigListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Duration.class))) {
      return DurationListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Boolean.class))) {
      return BooleanListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Integer.class))) {
      return IntListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Long.class))) {
      return LongListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Number.class))) {
      return NumberListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Double.class))) {
      return DoubleListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, String.class))) {
      return StringListM.class.getCanonicalName();
    }
    if (type.equals(ParameterizedTypeName.get(List.class, Object.class))) {
      return AnyRefListM.class.getCanonicalName();
    }

    throw new AnnotationProcessingException(
        String.format("Unsupported config value type '%s'. Please provide a custom mapper", type),
        abstractMethod
    );
  }

}
