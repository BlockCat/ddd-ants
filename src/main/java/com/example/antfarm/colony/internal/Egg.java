package com.example.antfarm.colony.internal;

/**
 * One egg in the brood. Matures into an adult after {@code eggTicks}
 * advances of the colony clock.
 */
public final class Egg {

	private int ticksLeft;

	public Egg(int ticksLeft) {
		this.ticksLeft = ticksLeft;
	}

	/** Advances the egg one tick; returns {@code true} when it has matured. */
	public boolean advance() {
		return --ticksLeft <= 0;
	}
}
