package com.mylaesoftware.annotations;

import com.mylaesoftware.mappers.ConfigMapper;
import com.mylaesoftware.mappers.NoMapper;
import com.mylaesoftware.validators.ConfigValidator;
import com.mylaesoftware.validators.NoValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used for methods of interfaces annotated with {@link ConfigType @ConfgType}.
 *
 * <p>It generates the necessary code to read the property at the specified path and assign it to a constant.</p>
 *
 * <p>Unless a custom mapper is specified, one of the basic mapper matching the return type of the method is used
 * to parse the property. If no satisfying mapper is found the compilation will fail with an error. </p>
 *
 * @author Claudio Scandura
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigValue {

  /**
   * The path of the property where to find this config value.
   *
   * @return The path of the property where to find this config value
   */
  String atPath();

  /**
   * Optional custom mapper to use to parse the config value.
   *
   * @return The custom mapper to use to parse the config value
   */
  Class<? extends ConfigMapper<?>> mappedBy() default NoMapper.class;

  /**
   * Optional validator to use verify the parsed value.
   *
   * @return The validator to use verify the parsed value
   */
  Class<? extends ConfigValidator<?>>[] validatedBy() default NoValidation.class;
}
