package com.example.antfarm.predators.internal;

import com.example.antfarm.predators.BirdId;
import com.example.antfarm.world.Position;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * One bird. Internal to the predators module; the public surface is
 * {@code PredatorService}.
 *
 * Birds fly above the terrain (their position never blocks or is blocked by
 * ground dwellers). Between hunts a bird keeps a cooldown — after striking
 * it is satisfied and will not hunt again until the interval has passed.
 */
@com.example.ddd.DDDEntity
@Accessors(fluent = true)
public final class Bird {

	@Getter
	private final BirdId id;
	@Getter
	@Setter
	private Position position;
	private long nextHuntTick = 0;

	public Bird(BirdId id, Position position, long firstHuntDelay) {
		this.id = id;
		this.position = position;
		this.nextHuntTick = firstHuntDelay;
	}

	public boolean readyToHunt(long tick) {
		return tick >= nextHuntTick;
	}

	/** Arms the next hunt after {@code interval} ticks (or now if {@code interval <= 0}). */
	public void armHunt(long tick, int interval) {
		this.nextHuntTick = tick + interval;
	}
}
