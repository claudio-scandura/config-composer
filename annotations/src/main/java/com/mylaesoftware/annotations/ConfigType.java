package com.mylaesoftware.annotations;

import com.mylaesoftware.validators.ConfigValidator;
import com.mylaesoftware.validators.NoValidation;
import com.typesafe.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used to mark interfaces as config types
 *
 * <p>It generates the necessary code to implement the annotated interface.
 *
 * @author Claudio Scandura
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigType {

  /**
   * The optional context path of this config type.
   *
   * @return context path of this config type
   */
  String contextPath() default "";

  /**
   * Optional validator to use verify the parsed config type.
   *
   * @return The validator to use verify the config type
   */
  Class<? extends ConfigValidator<?>>[] validatedBy() default NoValidation.class;

  /**
   * Indicates whether {@link ConfigValue ConfigValues} parsing in this <tt>ConfigType</tt> should fall back to
   * {@link com.typesafe.config.ConfigBeanFactory#create(Config, Class)} when a mapper cannot be found.
   *
   * @return true when mapping should default to {@link com.typesafe.config.ConfigBeanFactory#create(Config, Class)}
   *         when a mapper cannot be found otherwise false.
   */
  boolean fallbackToBeanMapper() default false;
}
