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
  private Map<String, FactoryClass> factoryClasses = new LinkedHashMap<String, FactoryClass>();

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    typeUtils = processingEnv.getTypeUtils();
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
  }

  @Override public Set<String> getSupportedAnnotationTypes() {
    Set<String> annotataions = new LinkedHashSet<String>();
    annotataions.add(Factory.class.getCanonicalName());
    return annotataions;
  }

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /**
   * Checks if the annotated element is a class, has an empty constructor and the required super
   * class
   */
  private boolean isValidClass(FactoryItem item) {

    // Cast to TypeElement, has more type specific methods
    TypeElement classElement = item.getAnnotatedClassElement();

    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
      error(classElement, "The class %s is not public.",
          classElement.getQualifiedName().toString());
      return false;
    }

    // Check if it's an abstract class
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      error(classElement, "The class %s is abstract. You can't annotate abstract classes with @%",
          classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
      return false;
    }

    // Check inheritance: Class must be childclass as specified in @Factory.type();
    TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedSuperClassName());
    if (superClassElement.getKind() == ElementKind.INTERFACE) {
      // Check interface implemented
      if (!classElement.getInterfaces().contains(superClassElement.asType())) {
        error(classElement, "The class %s annotated with @%s must implement the interface %s",
            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
            item.getQualifiedSuperClassName());
        return false;
      }
    } else {
      // Check subclassing
      TypeElement currentClass = classElement;
      while (true) {
        TypeMirror superClassType = currentClass.getSuperclass();

        if (superClassType.getKind() == TypeKind.NONE) {
          // Basis class (java.lang.Object) reached, so exit
          error(classElement, "The class %s annotated with @%s must inherit from %s",
              classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
              item.getQualifiedSuperClassName());
          return false;
        }

        if (superClassType.toString().equals(item.getQualifiedSuperClassName())) {
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
          return true;
        }
      }
    }

    // No empty constructor found
    error(classElement, "The class %s must provide an public empty default constructor",
        classElement.getQualifiedName().toString());
    return false;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

      // Check if a class has been annotated with @Factory
      if (annotatedElement.getKind() != ElementKind.CLASS) {
        error(annotatedElement, "Only classes can be annotated with @%s",
            Factory.class.getSimpleName());
        return false; // Exit processing
      }

      // We can cast it, because we know that it of ElementKind.CLASS
      TypeElement typeElement = (TypeElement) annotatedElement;

      try {
        FactoryItem factoryItem = new FactoryItem(typeElement); // throws IllegalArgumentException

        if (!isValidClass(factoryItem)) {
          return false; // Error message printed, exit processing
        }

        // Everything is fine, so try to add
        FactoryClass factoryClass = factoryClasses.get(factoryItem.getQualifiedSuperClassName());
        if (factoryClass == null) {
          factoryClass = new FactoryClass();
          factoryClasses.put(factoryItem.getQualifiedSuperClassName(), factoryClass);
        }

        // Check if id is conflicting with another @Factory annotated class with the same id
        FactoryItem alreadyFound = factoryClass.add(factoryItem);
        if (alreadyFound != null) {
          error(annotatedElement,
              "Conflict: The class %s is annotated with @%s with id ='%s' but %s already uses the same id",
              typeElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
              alreadyFound.getAnnotatedClassElement().getQualifiedName().toString());

          return false;
        }
      } catch (IllegalArgumentException e) {
        // Another approach of handling exceptions and printing error messages
        error(typeElement, e.getMessage());
      }
    }

    // All elements processed, everything is fine, so generate Factory code
    if (roundEnv.processingOver()) {
      try {
        for (FactoryClass factoryClass : factoryClasses.values()) {
          factoryClass.generateCode(elementUtils, filer);
        }
      } catch (IOException e) {
        error(null, e.getMessage());
      }
    }

    return false;
  }

  /**
   * Prints an error message
   *
   * @param e The element which has caused the error. Can be null
   * @param msg The error message
   * @param args if the error messge cotains %s, %d etc. placeholders this arguments will be used
   * to
   * replace them
   */
  private void error(Element e, String msg, Object... args) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
  }
}
