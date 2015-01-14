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

/**
 * This Exception will be thrown to indicate that the the given {@link Factory#id()} is already used
 * by another {@link FactoryAnnotatedClass}
 *
 * @author Hannes Dorfmann
 */
public class IdAlreadyUsedException extends Exception {

  private FactoryAnnotatedClass existing;

  public IdAlreadyUsedException(FactoryAnnotatedClass existing) {
    this.existing = existing;
  }

  public FactoryAnnotatedClass getExisting() {
    return existing;
  }
}
