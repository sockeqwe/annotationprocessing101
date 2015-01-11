package com.hannesdorfmann.annotationprocessing101.factory;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

/**
 * @author Hannes Dorfmann
 */

@Factory(
    id = "Margherita",
    type = Meal.class
)
public class MargheritaPizza implements Meal {

  @Override public float getPrice() {
    return 6f;
  }
}
