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
 * This class holds all {@link FactoryAnnotatedClass}s that belongs to one factory. In other words,
 * this class holds a list with all @Factory annotated classes. This class also checks if the id of
 * each @Factory annotated class is unique.
 *
 * @author Hannes Dorfmann
 */
public class FactoryGroupedClasses {

  /**
   * Will be added to the name of the generated factory class
   */
  private static final String SUFFIX = "Factory";

  private String qualifiedClassName;

  private Map<String, FactoryAnnotatedClass> itemsMap =
      new LinkedHashMap<String, FactoryAnnotatedClass>();

  public FactoryGroupedClasses(String qualifiedClassName) {
    this.qualifiedClassName = qualifiedClassName;
  }

  /**
   * Adds an annotated class to this factory.
   *
   * @throws ProcessingException if another annotated class with the same id is
   * already present.
   */
  public void add(FactoryAnnotatedClass toInsert) throws ProcessingException {

    FactoryAnnotatedClass existing = itemsMap.get(toInsert.getId());
    if (existing != null) {

      // Alredy existing
      throw new ProcessingException(toInsert.getTypeElement(),
          "Conflict: The class %s is annotated with @%s with id ='%s' but %s already uses the same id",
          toInsert.getTypeElement().getQualifiedName().toString(), Factory.class.getSimpleName(),
          toInsert.getId(), existing.getTypeElement().getQualifiedName().toString());
    }

    itemsMap.put(toInsert.getId(), toInsert);
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

    for (FactoryAnnotatedClass item : itemsMap.values()) {
      jw.beginControlFlow("if (\"%s\".equals(id))", item.getId());
      jw.emitStatement("return new %s()", item.getTypeElement().getQualifiedName().toString());
      jw.endControlFlow();
      jw.emitEmptyLine();
    }

    jw.emitStatement("throw new IllegalArgumentException(\"Unknown id = \" + id)");
    jw.endMethod();

    jw.endType();

    jw.close();
  }
}
