package com.example.antfarm.colony;

/**
 * A hungry ant asked the colony store for a meal. Published by the ants
 * context; consumed by the colony context, which either grants it
 * (publishing {@link FoodGranted}) or ignores it when the store cannot
 * cover the meal. Carries a raw {@code long} ant id so the colony context
 * never depends on the ants context.
 */
@com.example.ddd.DDDEvent
public record FoodConsumptionRequested(ColonyId colonyId, long antId, double amount, long tick) {
}
