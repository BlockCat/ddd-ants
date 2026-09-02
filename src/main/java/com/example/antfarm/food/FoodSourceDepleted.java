package com.example.antfarm.food;

import com.example.antfarm.world.Position;

/**
 * A food source ran out of food and was removed from the terrain.
 */
@com.example.ddd.DDDEvent
public record FoodSourceDepleted(FoodId foodId, Position position, long tick) {
}
