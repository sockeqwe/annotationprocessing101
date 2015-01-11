package com.hannesdorfmann.annotationprocessing101.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Hannes Dorfmann
 */
public class PizzaStore {

  private MealFactory factory = new MealFactory();

  public Meal order(String mealName) {
    return factory.create(mealName);
    //return null;
  }

  private static String readConsole() throws IOException {
    System.out.println("What do you like?");
    BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
    String input = bufferRead.readLine();
    return input;
  }

  public static void main(String[] args) throws IOException {
    PizzaStore pizzaStore = new PizzaStore();
    Meal meal = pizzaStore.order(readConsole());
    System.out.println("Bill: $" + meal.getPrice());
  }
}
