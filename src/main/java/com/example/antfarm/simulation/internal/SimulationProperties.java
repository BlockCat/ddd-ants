package com.example.antfarm.simulation.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Engine configuration, bound from {@code simulation.*} in
 * {@code application.properties}.
 *
 * @param tickIntervalMs milliseconds between ticks (e.g. 100 = 10 ticks/s)
 * @param randomSeed     seed for all randomness (deterministic runs)
 */
@ConfigurationProperties(prefix = "simulation")
@com.example.ddd.DDDValueObject
public record SimulationProperties(
		int tickIntervalMs,
		long randomSeed,
		World world,
		Colony colony,
		Ant ant,
		Food food,
		Predators predators) {

	public record World(int width, int height, int branches, int pebbles) {
	}

	public record Colony(int initialFood, double eggCost, int eggTicks, int broodCap,
			int layCooldownTicks, int initialWorkers, int initialForagers) {
	}

	public record Ant(double startEnergy, double outsideCost, double insideCost,
			double eatThreshold, double mealAmount, double leaveThreshold,
			int leaveIntervalTicks, int minExploreTicks, int maxExploreTicks,
			double carryingCapacity, int sniffRadius, double digProbability, double digCost) {
	}

	public record Food(int maxSources, int spawnIntervalTicks, double minAmount, double maxAmount) {
	}

	public record Predators(int birdCount, int huntIntervalTicks, int huntRadius, int moveEveryTicks) {
	}
}
