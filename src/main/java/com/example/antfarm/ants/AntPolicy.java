package com.example.antfarm.ants;

/**
 * Behaviour knobs for adult ants, decided by the engine at startup (they
 * come from {@code simulation.ant.*} properties).
 *
 * @param startEnergy        energy of a freshly hatched ant
 * @param outsideCost        energy spent per tick outside the nest
 * @param insideCost         energy spent per tick inside the nest
 * @param eatThreshold       below this energy an ant tries to eat
 * @param mealAmount         food taken from the store per meal
 * @param leaveThreshold     energy at which a fed ant leaves again
 * @param leaveIntervalTicks min ticks inside before leaving again
 * @param minExploreTicks    min duration of an exploration trip
 * @param maxExploreTicks    max duration of an exploration trip
 * @param carryingCapacity   how much food an ant can carry home per trip
 * @param sniffRadius        how far an ant smells food scent (gradient radius)
 * @param digProbability     per-tick chance a worker digs a chamber (workers only)
 * @param digCost            energy cost of digging one chamber
 */
@com.example.ddd.DDDValueObject
public record AntPolicy(double startEnergy, double outsideCost, double insideCost,
		double eatThreshold, double mealAmount, double leaveThreshold,
		int leaveIntervalTicks, int minExploreTicks, int maxExploreTicks,
		double carryingCapacity, int sniffRadius, double digProbability, double digCost) {

	public static final AntPolicy DEFAULTS = new AntPolicy(
			100, 0.2, 0.05, 35, 70, 90, 25, 60, 400,
			15, 18, 0.008, 0.6);
}
