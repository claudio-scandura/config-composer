package com.mylaesoftware.specs;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.typesafe.config.Config;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.Set;
import java.util.function.BinaryOperator;

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
  final Set<ConfigValueSpec> configValues;

  private ConfigTypeSpec() {
    packageName = "";
    superInterfaces = emptySet();
    configValues = emptySet();
  }

  public ConfigTypeSpec(String packageName, Set<TypeMirror> superInterfaces, Set<ConfigValueSpec> configValues) {
    this.packageName = packageName;
    this.superInterfaces = superInterfaces;
    this.configValues = configValues;
  }

  public String packageName() {
    return packageName;
  }

  public TypeSpec build() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(ConfigTypeSpec.CLASS_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterfaces(superInterfaces.stream().map(TypeName::get).collect(toList()));

    return configValues.parallelStream().reduce(builder,
        (b, spec) -> b.addField(spec.getField()).addMethod(spec.getInitMethod()).addMethod(spec.getOverrideMethod()),
        specMerger)
        .addMethod(buildConstructor())
        .build();
  }

  private MethodSpec buildConstructor() {
    final MethodSpec.Builder builder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(Config.class, "config", Modifier.FINAL);

    return configValues.stream().collect(() -> builder,
        (b, reader) -> b.addStatement(reader.getField().name + " = " + reader.getInitMethod().name + "($N)", "config"),
        (l, r) -> l.addCode(r.build().code)
    ).build();
  }

  public static ConfigTypeSpec empty() {
    return EMPTY;
  }

  public boolean isEmpty() {
    return this.equals(EMPTY);
  }

}
