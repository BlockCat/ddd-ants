package com.example.antfarm.colony;

/**
 * The colony store granted a meal to a hungry ant. Published by the colony
 * context in reaction to {@link FoodConsumptionRequested}; the ants context
 * listens to it and refills the ant's energy. Carries a raw {@code long} ant
 * id so the colony context never depends on the ants context.
 */
@com.example.ddd.DDDEvent
public record FoodGranted(ColonyId colonyId, long antId, double amount, long tick) {
}
