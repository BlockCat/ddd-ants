package com.example.antfarm.ants;

import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.world.Position;

/**
 * A worker dug a new chamber out of the sand. Owned by the ants context.
 */
@com.example.ddd.DDDEvent
public record ChamberDug(AntId antId, ColonyId colonyId, Position position, long tick) {
}
