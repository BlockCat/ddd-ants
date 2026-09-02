package com.example.antfarm.world;

/**
 * Kind of a world cell. {@code SAND} and {@code CHAMBER} are walkable;
 * {@code BRANCH} and {@code PEBBLE} are obstacles.
 */
public enum TerrainKind {

	SAND(true),
	BRANCH(false),
	PEBBLE(false),
	CHAMBER(true);

	private final boolean walkable;

	TerrainKind(boolean walkable) {
		this.walkable = walkable;
	}

	public boolean isWalkable() {
		return walkable;
	}
}
