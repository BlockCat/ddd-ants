package com.example.antfarm.colony;

/**
 * The queen laid an egg because the colony store could afford it.
 */
@com.example.ddd.DDDEvent
public record EggLaid(ColonyId colonyId, long tick) {
}
