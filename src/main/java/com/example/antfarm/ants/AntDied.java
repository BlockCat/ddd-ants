package com.example.antfarm.ants;

import com.example.antfarm.colony.ColonyId;

/**
 * An ant died. Owned and published by the ants context.
 */
@com.example.ddd.DDDEvent
public record AntDied(AntId antId, ColonyId colonyId, AntDeathCause cause, long tick) {
}
