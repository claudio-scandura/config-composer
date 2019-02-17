package com.mylaesoftware;

import com.mylaesoftware.annotations.ConfigType;
import com.mylaesoftware.annotations.ConfigValue;
import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.specs.ConfigTypeSpec;
import com.mylaesoftware.specs.ConfigTypeSpecReducer;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mylaesoftware.Annotations.CONFIG_TYPE;
import static com.mylaesoftware.Annotations.CONFIG_VALUE;

public class ConfigProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private ConfigTypeSpecReducer reducer;
  private final Set<Element> annotatedClasses = Collections.synchronizedSet(new HashSet<>());

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    reducer = new ConfigTypeSpecReducer(processingEnv.getTypeUtils());
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(
        ConfigValue.class.getCanonicalName(),
        ConfigType.class.getCanonicalName()
    ).collect(Collectors.toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!annotatedClasses.addAll(roundEnv.getElementsAnnotatedWith(ConfigType.class)) && !annotatedClasses.isEmpty()) {
      return true;
    }
    try {
      validateConfigValueAnnotatedElements(roundEnv.getElementsAnnotatedWith(ConfigValue.class));

      ConfigTypeSpec configClass = annotatedClasses.parallelStream()
          .map(e -> (TypeElement) e)
          .reduce(ConfigTypeSpec.empty(), reducer::accumulate, reducer::combine);

      JavaFile.builder(configClass.packageName(), configClass.build()).build().writeTo(filer);
    } catch (AnnotationProcessingException ape) {
      messager.printMessage(Kind.ERROR, ape.getMessage());
    } catch (IOException ioe) {
      messager.printMessage(Kind.WARNING, ioe.getMessage());
    }
    return true;
  }

  private void validateConfigValueAnnotatedElements(Set<? extends Element> elements) {
    List<Element> nonMethods = elements.stream()
        .filter(e -> !e.getKind().equals(ElementKind.METHOD))
        .collect(Collectors.toList());

    if (!nonMethods.isEmpty()) {
      throw new AnnotationProcessingException(
          CONFIG_VALUE.name + " can only be used on methods",
          nonMethods
      );
    }

    List<Element> notInConfigTypes = elements.stream()
        .filter(e -> Objects.isNull(e.getEnclosingElement().getAnnotation(CONFIG_TYPE.annotation())))
        .collect(Collectors.toList());

    if (!notInConfigTypes.isEmpty()) {
      throw new AnnotationProcessingException(
          CONFIG_VALUE.name + " needs to be enclosed by a type annotated with " + CONFIG_TYPE.name,
          notInConfigTypes
      );
    }

    List<Element> duplicates = elements.stream()
        .collect(Collectors.groupingBy(Element::getSimpleName))
        .values().stream().filter(list -> list.size() > 1)
        .flatMap(List::stream)
        .collect(Collectors.toList());

    if (!duplicates.isEmpty()) {
      throw new AnnotationProcessingException(
          CONFIG_VALUE.name  +
              " cannot be used on multiple methods with the same name in different config type definitions",
          duplicates
      );
    }
  }
}
