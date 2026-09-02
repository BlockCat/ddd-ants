package com.example.antfarm.colony;

import com.example.antfarm.world.Position;

/**
 * Output of {@code ColonyService.advance}: brood matured and is ready to
 * become a roaming ant. The engine mediates the hand-over to the ants
 * context, which assigns the actual ant identity.
 */
public record HatchRequest(ColonyId colonyId, Role role, Position entrance) {
}
