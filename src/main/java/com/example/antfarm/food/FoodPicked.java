package com.example.antfarm.food;

/**
 * Food was actually picked up from a source by a forager. Published by the
 * food context in reaction to {@link FoodPickupRequested}; the ants context
 * listens to it and puts the amount into the ant's load. Carries a raw
 * {@code long} ant id so the food context never depends on the ants context.
 */
@com.example.ddd.DDDEvent
public record FoodPicked(long antId, FoodId foodId, double amount, long tick) {
}
