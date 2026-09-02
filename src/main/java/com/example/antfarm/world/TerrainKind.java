package com.example.antfarm.world;

/**
 * Kind of a world cell.
 *
 * <p><b>Surface:</b> {@code SAND} and {@code HOLE} are walkable open ground;
 * {@code BRANCH} and {@code PEBBLE} are obstacles. A {@code HOLE} is an
 * entrance/exit of the burrow — ants may pass between it and an adjacent
 * underground cell.
 *
 * <p><b>Underground (the burrow):</b> {@code TUNNEL} corridors and
 * {@code CHAMBER} rooms are carved out of the sand by workers. Only ants
 * that are already in the burrow can stand on them; ants move faster there.
 *
 * <p>Ordinal order is part of the renderer contract
 * ({@code terrainRows} / the web client).
 */
public enum TerrainKind {

	SAND(true),
	BRANCH(false),
	PEBBLE(false),
	HOLE(true),
	TUNNEL(true),
	CHAMBER(true);

	private final boolean walkable;

	TerrainKind(boolean walkable) {
		this.walkable = walkable;
	}

	public boolean isWalkable() {
		return walkable;
	}

	/** Underground cells belong to the nest burrow network. */
	public boolean isUnderground() {
		return this == TUNNEL || this == CHAMBER;
	}

	/** A burrow entrance/exit that connects surface and underground. */
	public boolean isHole() {
		return this == HOLE;
	}
}
