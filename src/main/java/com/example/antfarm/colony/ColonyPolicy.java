package com.example.antfarm.colony;

/**
 * Policy knobs for colony life, decided by the engine at colony creation
 * (they come from {@code simulation.colony.*} properties).
 *
 * @param eggCost           food withdrawn from the store when the queen lays an egg
 * @param eggTicks          ticks an egg needs to mature into an adult
 * @param broodCap          maximum concurrent brood the queen will maintain
 * @param layCooldownTicks  minimum ticks between two layings
 */
@com.example.ddd.DDDValueObject
public record ColonyPolicy(double eggCost, int eggTicks, int broodCap, int layCooldownTicks) {
}
