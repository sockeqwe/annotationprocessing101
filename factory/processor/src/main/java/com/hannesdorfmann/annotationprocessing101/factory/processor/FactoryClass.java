package com.hannesdorfmann.annotationprocessing101.factory.processor;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

/**
 * This class holds all {@link FactoryItem}s that belongs to one factory. In other words,
 * this class holds a list with all @Factory annotated classes. This class also checks if the id of
 * each @Factory annotated class is unique.
 *
 * @author Hannes Dorfmann
 */
public class FactoryClass {

  /**
   * Will be added to the name of the generated factory class
   */
  private static final String SUFFIX = "Factory";

  private String qualifiedClassName;

  private Map<String, FactoryItem> itemsMap = new LinkedHashMap<String, FactoryItem>();

  /**
   * Adds an item to this factory. This method will check if another item with the same id has
   * already present. In this case, the already existing one is returned and you should make an
   * error message out of it.
   */
  public FactoryItem add(FactoryItem toInsert) {

    // Save the qualified name once
    if (qualifiedClassName == null) {
      qualifiedClassName = toInsert.getQualifiedSuperClassName();
    }

    FactoryItem existing = itemsMap.get(toInsert.getId());
    if (existing != null) {
      return existing;
    }

    itemsMap.put(toInsert.getId(), toInsert);
    return null;
  }

  /**
   * Generate the java code
   *
   * @throws IOException
   */
  public void generateCode(Elements elementUtils, Filer filer) throws IOException {

    TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
    String factoryClassName = superClassName.getSimpleName() + SUFFIX;

    JavaFileObject jfo = filer.createSourceFile(qualifiedClassName + SUFFIX);
    Writer writer = jfo.openWriter();
    JavaWriter jw = new JavaWriter(writer);

    // Write package
    PackageElement pkg = elementUtils.getPackageOf(superClassName);
    if (!pkg.isUnnamed()) {
      jw.emitPackage(pkg.getQualifiedName().toString());
      jw.emitEmptyLine();
    } else {
      jw.emitPackage("");
    }

    jw.beginType(factoryClassName, "class", EnumSet.of(Modifier.PUBLIC));
    jw.emitEmptyLine();
    jw.beginMethod(qualifiedClassName, "create", EnumSet.of(Modifier.PUBLIC), "String", "id");

    jw.beginControlFlow("if (id == null)");
    jw.emitStatement("throw new IllegalArgumentException(\"id is null!\")");
    jw.endControlFlow();

    for (FactoryItem item : itemsMap.values()) {
      jw.beginControlFlow("if (\"%s\".equals(id))", item.getId());
      jw.emitStatement("return new %s()",
          item.getAnnotatedClassElement().getQualifiedName().toString());
      jw.endControlFlow();
      jw.emitEmptyLine();
    }

    jw.emitStatement("throw new IllegalArgumentException(\"Unknown id = \" + id)");
    jw.endMethod();

    jw.endType();

    jw.close();
  }
}
