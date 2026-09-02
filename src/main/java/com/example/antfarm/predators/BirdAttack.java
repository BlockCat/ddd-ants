package com.example.antfarm.predators;

/**
 * An attack a bird decided on during a tick. Returned by the predators
 * module so the engine can apply the effect (ant death) in the owning ants
 * context — engine-mediated, keeping module boundaries intact.
 */
public record BirdAttack(BirdId birdId, long antId) {
}
