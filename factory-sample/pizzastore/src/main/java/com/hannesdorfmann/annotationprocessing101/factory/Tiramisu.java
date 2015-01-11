package com.hannesdorfmann.annotationprocessing101.factory;

import com.hannesdorfmann.annotationprocessing101.factory.annotation.Factory;

/**
 * @author Hannes Dorfmann
 */

@Factory(
    id = "Tiramisu",
    type = Meal.class
)
public class Tiramisu implements Meal {

  @Override public float getPrice() {
    return 4.5f;
  }
}
