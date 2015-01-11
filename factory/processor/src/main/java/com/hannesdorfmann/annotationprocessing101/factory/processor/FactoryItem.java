package com.hannesdorfmann.annotationprocessing101.factory.processor;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import org.apache.commons.lang3.StringUtils;

/**
 * Holds the information about a class annotated with @Factory
 *
 * @author Hannes Dorfmann
 */
public class FactoryItem {

  private TypeElement annotatedClassElement;
  private String qualifiedSuperClassName;
  private String simpleTypeName;
  private String id;

  public FactoryItem(TypeElement classElement) throws IllegalArgumentException {
    this.annotatedClassElement = classElement;
    Factory annotation = classElement.getAnnotation(Factory.class);
    id = annotation.id();

    if (StringUtils.isEmpty(id)) {
      throw new IllegalArgumentException(
          String.format("id() in @%s for class %s is null or empty! that's not allowed",
              Factory.class.getSimpleName(), classElement.getQualifiedName().toString()));
    }

    // Get the full QualifiedTypeName
    try {
      Class<?> clazz = annotation.type();
      qualifiedSuperClassName = clazz.getCanonicalName();
      simpleTypeName = clazz.getSimpleName();
    } catch (MirroredTypeException mte) {
      DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
      TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
      qualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
      simpleTypeName = classTypeElement.getSimpleName().toString();
    }
  }

  /**
   * Get the id as specified in {@link Factory#id()}.
   * return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Get the full qualified name of the type specified in  {@link Factory#type()}.
   *
   * @return qualified name
   */
  public String getQualifiedSuperClassName() {
    return qualifiedSuperClassName;
  }


  /**
   * Get the simple name of the type specified in  {@link Factory#type()}.
   *
   * @return qualified name
   */
  public String getSimpleSuperClassName() {
    return simpleTypeName;
  }

  /**
   * The original element that was annotated with @Factory
   */
  public TypeElement getAnnotatedClassElement() {
    return annotatedClassElement;
  }
}
