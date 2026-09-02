package com.example.antfarm.simulation;

import java.util.List;

/**
 * Immutable view of the whole simulation at one tick, assembled from the
 * module public APIs for rendering. Value-only records (no domain object
 * references) so it serialises cleanly to JSON over SSE/REST.
 */
public record SimulationSnapshot(
		long tick,
		boolean running,
		double ticksPerSecond,
		int worldWidth,
		int worldHeight,
		int antsAlive,
		int workers,
		int foragers,
		double colonyFood,
		int brood,
		int foodSources,
		int birdCount,
		List<Ant> ants,
		List<Nest> nests,
		List<Food> foods,
		List<Bird> birds) {

	public record Ant(long id, String role, int x, int y, double energy, double carrying) {
	}

	public record Nest(long colonyId, int x, int y) {
	}

	public record Food(long id, int x, int y, double amount) {
	}

	public record Bird(long id, int x, int y) {
	}

	public static SimulationSnapshot empty(long tick, int width, int height) {
		return new SimulationSnapshot(tick, false, 0, width, height, 0, 0, 0, 0, 0, 0, 0,
				List.of(), List.of(), List.of(), List.of());
	}
}
