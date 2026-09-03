package com.example.antfarm.ants;

/**
 * Why an ant died. Published with {@code AntDied} by the ants context — the
 * context that owns the ant's life cycle — even when the cause originated
 * elsewhere (a bird attack is recorded as {@code EATEN} after the simulation
 * context translates the predators fact into this context's kill command).
 */
public enum AntDeathCause {
	STARVED,
	OLD_AGE,
	EATEN
}
