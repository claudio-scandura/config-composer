package com.mylaesoftware.specs;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.typesafe.config.Config;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

import java.util.Set;

import static com.mylaesoftware.specs.ConfigSpecReducer.specMerger;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

public class ConfigSpec {

  private static final ConfigSpec EMPTY = new ConfigSpec();

  public static final String className = "GlobalConfig";

  final String packageName;
  final Set<TypeMirror> superInterfaces;
  final Set<ConfigValueSpec> configValues;

  private ConfigSpec() {
    packageName = "";
    superInterfaces = emptySet();
    configValues = emptySet();
  }

  public ConfigSpec(String packageName, Set<TypeMirror> superInterfaces, Set<ConfigValueSpec> configValues) {
    this.packageName = packageName;
    this.superInterfaces = superInterfaces;
    this.configValues = configValues;
  }

  public String packageName() {
    return packageName;
  }

  public TypeSpec build() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(ConfigSpec.className)
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

  public static ConfigSpec empty() {
    return EMPTY;
  }

  public boolean isEmpty() {
    return this.equals(EMPTY);
  }

}
