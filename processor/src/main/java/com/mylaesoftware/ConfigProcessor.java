package com.mylaesoftware;

import com.mylaesoftware.exceptions.AnnotationProcessingException;
import com.mylaesoftware.specs.ConfigSpec;
import com.mylaesoftware.specs.ConfigSpecReducer;
import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private final Set<Element> annotatedClasses = Collections.synchronizedSet(new HashSet<>());

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Stream.of(
        ConfigValue.class.getCanonicalName(),
        Config.class.getCanonicalName()
    ).collect(Collectors.toSet());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_8;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!annotatedClasses.addAll(roundEnv.getElementsAnnotatedWith(Config.class))) {
      return true;
    }

    try {

      ConfigSpec configClass = annotatedClasses.parallelStream()
          .map(e -> (TypeElement) e)
          .reduce(ConfigSpec.empty(), ConfigSpecReducer::accumulate, ConfigSpecReducer::combine);

      JavaFile.builder(configClass.packageName(), configClass.build()).build().writeTo(filer);
    } catch (AnnotationProcessingException ape) {
      messager.printMessage(Kind.ERROR, ape.getMessage());
    } catch (IOException ioe) {
      messager.printMessage(Kind.WARNING, ioe.getMessage());
    }
    return true;
  }
}
