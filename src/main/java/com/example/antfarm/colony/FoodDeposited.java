package com.example.antfarm.colony;

/**
 * A forager delivered carried food into the colony store. Published by the
 * colony context (it owns the store). {@code antId} is the raw id value of
 * the delivering ant (a {@code long} so colony need not depend on ants).
 */
@com.example.ddd.DDDEvent
public record FoodDeposited(ColonyId colonyId, long antId, double amount, double storeAfter, long tick) {
}
