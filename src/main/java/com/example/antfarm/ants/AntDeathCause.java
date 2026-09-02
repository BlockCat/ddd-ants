package com.example.antfarm.ants;

/**
 * Why an ant died. Published with {@code AntDied} by the ants context — the
 * context that owns the ant's life cycle — even when the cause originated
 * elsewhere (e.g. a bird attack will be recorded as {@code EATEN} once the
 * predators context exists).
 */
public enum AntDeathCause {
	STARVED,
	OLD_AGE,
	EATEN
}
