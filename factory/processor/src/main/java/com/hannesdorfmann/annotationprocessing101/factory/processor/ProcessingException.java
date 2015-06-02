package com.hannesdorfmann.annotationprocessing101.factory.processor;

import javax.lang.model.element.Element;

/**
 * @author Hannes Dorfmann
 */
public class ProcessingException extends Exception {

  Element element;

  public ProcessingException(Element element, String msg, Object... args) {
    super(String.format(msg, args));
    this.element = element;
  }

  public Element getElement() {
    return element;
  }
}
