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

import com.google.auto.service.AutoService;
import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Annotation Processor for @Factory annotation
 *
 * @author Hannes Dorfmann
 */
@AutoService(Processor.class) public class FactoryProcessor extends AbstractProcessor {

  private Types typeUtils;
  private Elements elementUtils;
  private Filer filer;
  private Messager messager;
  private Map<String, FactoryGroupedClasses> factoryClasses =
      new LinkedHashMap<String, FactoryGroupedClasses>();

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotations = new LinkedHashSet<String>();
    annotations.add(Factory.class.getCanonicalName());
    return annotations;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /**
   * Checks if the annotated element observes our rules
   */
  private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {

    // Cast to TypeElement, has more type specific methods
    TypeElement classElement = item.getTypeElement();

    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
      throw new ProcessingException(classElement, "The class %s is not public.",
          classElement.getQualifiedName().toString());
    }

    // Check if it's an abstract class
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      throw new ProcessingException(classElement,
          "The class %s is abstract. You can't annotate abstract classes with @%",
          classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
    }

    // Check inheritance: Class must be childclass as specified in @Factory.type();
    TypeElement superClassElement =
        elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
    if (superClassElement.getKind() == ElementKind.INTERFACE) {
      // Check interface implemented
      if (!classElement.getInterfaces().contains(superClassElement.asType())) {
        throw new ProcessingException(classElement,
            "The class %s annotated with @%s must implement the interface %s",
            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
            item.getQualifiedFactoryGroupName());
      }
    } else {
      // Check subclassing
      TypeElement currentClass = classElement;
      while (true) {
        TypeMirror superClassType = currentClass.getSuperclass();

        if (superClassType.getKind() == TypeKind.NONE) {
          // Basis class (java.lang.Object) reached, so exit
          throw new ProcessingException(classElement,
              "The class %s annotated with @%s must inherit from %s",
              classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
              item.getQualifiedFactoryGroupName());
        }

        if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
          // Required super class found
          break;
        }

        // Moving up in inheritance tree
        currentClass = (TypeElement) typeUtils.asElement(superClassType);
      }
    }

    // Check if an empty public constructor is given
    for (Element enclosed : classElement.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
        ExecutableElement constructorElement = (ExecutableElement) enclosed;
        if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers()
            .contains(Modifier.PUBLIC)) {
          // Found an empty constructor
          return;
        }
      }
    }

    // No empty constructor found
    throw new ProcessingException(classElement,
        "The class %s must provide an public empty default constructor",
        classElement.getQualifiedName().toString());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    try {

      // Scan classes
      for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

        // Check if a class has been annotated with @Factory
        if (annotatedElement.getKind() != ElementKind.CLASS) {
          throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
              Factory.class.getSimpleName());
        }

        // We can cast it, because we know that it of ElementKind.CLASS
        TypeElement typeElement = (TypeElement) annotatedElement;

        FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);

        checkValidClass(annotatedClass);

        // Everything is fine, so try to add
        FactoryGroupedClasses factoryClass =
            factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
        if (factoryClass == null) {
          String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
          factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
          factoryClasses.put(qualifiedGroupName, factoryClass);
        }

        // Checks if id is conflicting with another @Factory annotated class with the same id
        factoryClass.add(annotatedClass);
      }

      // Generate code
      for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
        factoryClass.generateCode(elementUtils, filer);
      }
      factoryClasses.clear();
    } catch (ProcessingException e) {
      error(e.getElement(), e.getMessage());
    } catch (IOException e) {
      error(null, e.getMessage());
    }

    return true;
  }

  /**
   * Prints an error message
   *
   * @param e The element which has caused the error. Can be null
   * @param msg The error message
   */
  public void error(Element e, String msg) {
    messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
  }
}
