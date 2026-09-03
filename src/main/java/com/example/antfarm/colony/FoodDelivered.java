package com.example.antfarm.colony;

/**
 * A forager delivered carried food to the nest. Published by the ants
 * context; consumed by the colony context, which adds it to the store and
 * then publishes the {@link FoodDeposited} fact. Carries a raw {@code long}
 * ant id so the colony context never depends on the ants context.
 */
@com.example.ddd.DDDEvent
public record FoodDelivered(ColonyId colonyId, long antId, double amount, long tick) {
}
