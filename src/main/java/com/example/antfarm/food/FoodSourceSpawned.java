package com.example.antfarm.food;

import com.example.antfarm.world.Position;

/**
 * A new food source appeared on the terrain.
 */
@com.example.ddd.DDDEvent
public record FoodSourceSpawned(FoodId foodId, Position position, FoodType type, double amount, long tick) {
}
