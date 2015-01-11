package com.hannesdorfmann.annotationprocessing101.factory;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

/**
 * @author Hannes Dorfmann
 */
@Factory(
    id = "Calzone",
    type = Meal.class
)
public class CalzonePizza implements Meal {

  @Override public float getPrice() {
    return 8.5f;
  }
}
