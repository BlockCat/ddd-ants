package com.example.antfarm.food;

/**
 * A forager standing on a food source asked to pick some of it up. Published
 * by the ants context; consumed by the food context, which draws the source
 * down and answers with {@link FoodPicked} (or nothing if the source is
 * already gone). Carries a raw {@code long} ant id so the food context never
 * depends on the ants context.
 */
@com.example.ddd.DDDEvent
public record FoodPickupRequested(long antId, FoodId foodId, double requested, long tick) {
}
