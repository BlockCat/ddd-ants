package com.example.antfarm.colony;

import com.example.antfarm.world.Position;

/**
 * Brood matured into an adult of a role at the nest entrance. The actual
 * roaming ant is created by the ants context (mediated by the engine); this
 * event is the colony's own record of the fact.
 */
@com.example.ddd.DDDEvent
public record AntHatched(ColonyId colonyId, Role role, Position entrance, long tick) {
}
