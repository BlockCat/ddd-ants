package com.example.antfarm.ants;

/**
 * An ant's momentum — the direction of travel carried between ticks.
 *
 * <p>Momentum is the movement bias that makes ant paths look like smooth
 * trails instead of a fresh random decision every tick: an ant prefers to
 * keep walking the way it walked last tick (straight ahead ≫ sideways ≫
 * U-turn). It is a <b>value object</b> shaped like a direction: immutable,
 * self-validating, each component a unit step in {@code -1..1}, and always
 * pointing at one of the eight neighbouring cells.
 *
 * @param dx horizontal step, {@code -1..1}
 * @param dy vertical step, {@code -1..1}
 */
@com.example.ddd.DDDValueObject
public record Momentum(int dx, int dy) {

	public static final Momentum NORTH = new Momentum(0, -1);

	public Momentum {
		if (dx < -1 || dx > 1 || dy < -1 || dy > 1) {
			throw new IllegalArgumentException("Momentum step out of range: (%d,%d)".formatted(dx, dy));
		}
		if (dx == 0 && dy == 0) {
			throw new IllegalArgumentException("Momentum needs a direction");
		}
	}

	/** Normalises an arbitrary delta to a unit step (the sign of each component). */
	public static Momentum of(int dx, int dy) {
		return new Momentum(Integer.compare(dx, 0), Integer.compare(dy, 0));
	}

	/**
	 * Dot product with another direction: {@code 1} = straight on,
	 * {@code 0} = sideways, {@code -1} = U-turn.
	 */
	public int dot(Momentum other) {
		return dx * other.dx + dy * other.dy;
	}
}
