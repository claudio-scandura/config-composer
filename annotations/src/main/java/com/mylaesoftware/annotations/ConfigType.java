package com.mylaesoftware.annotations;

import com.mylaesoftware.validators.ConfigValidator;
import com.mylaesoftware.validators.NoValidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigType {

  String contextPath() default "";

  Class<? extends ConfigValidator<?>>[] validatedBy() default NoValidation.class;
}
