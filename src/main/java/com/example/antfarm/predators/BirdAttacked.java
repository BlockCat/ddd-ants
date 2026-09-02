package com.example.antfarm.predators;

import com.example.antfarm.world.Position;

/**
 * A bird struck at an ant on open sand. The fact is owned by the predators
 * context; the effect on the ant (its death) is applied by the engine via
 * the ants context, so predators never mutates another module's aggregate.
 */
@com.example.ddd.DDDEvent
public record BirdAttacked(BirdId birdId, long antId, Position position, long tick) {
}
