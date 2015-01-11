package com.hannesdorfmann.annotationprocessing101.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate classes that are part of a certain factory
 *
 * @author Hannes Dorfmann
 */
@Target(ElementType.TYPE) @Retention(RetentionPolicy.CLASS)
public @interface Factory {

  /**
   * The name of the factory
   */
  Class type();

  /**
   * The identifier for determining which item should be instantiated
   */
  String id();
}
