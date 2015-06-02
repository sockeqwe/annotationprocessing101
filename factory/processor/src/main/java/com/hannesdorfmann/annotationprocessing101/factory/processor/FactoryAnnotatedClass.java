/*
 * Copyright (C) 2015 Hannes Dorfmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class FactoryAnnotatedClass {

  private TypeElement annotatedClassElement;
  private String qualifiedGroupClassName;
  private String simpleFactoryGroupName;
  private String id;

  /**
   * @throws ProcessingException if id() from annotation is null
   */
  public FactoryAnnotatedClass(TypeElement classElement) throws ProcessingException {
    this.annotatedClassElement = classElement;
    Factory annotation = classElement.getAnnotation(Factory.class);
    id = annotation.id();

    if (StringUtils.isEmpty(id)) {
      throw new ProcessingException(classElement,
          "id() in @%s for class %s is null or empty! that's not allowed",
          Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
    }

    // Get the full QualifiedTypeName
    try {
      Class<?> clazz = annotation.type();
      qualifiedGroupClassName = clazz.getCanonicalName();
      simpleFactoryGroupName = clazz.getSimpleName();
    } catch (MirroredTypeException mte) {
      DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
      TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
      qualifiedGroupClassName = classTypeElement.getQualifiedName().toString();
      simpleFactoryGroupName = classTypeElement.getSimpleName().toString();
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
  public String getQualifiedFactoryGroupName() {
    return qualifiedGroupClassName;
  }

  /**
   * Get the simple name of the type specified in  {@link Factory#type()}.
   *
   * @return qualified name
   */
  public String getSimpleFactoryGroupName() {
    return simpleFactoryGroupName;
  }

  /**
   * The original element that was annotated with @Factory
   */
  public TypeElement getTypeElement() {
    return annotatedClassElement;
  }
}
