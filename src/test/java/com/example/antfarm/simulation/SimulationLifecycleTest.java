package com.example.antfarm.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.antfarm.RecordingPublisher;
import com.example.antfarm.ants.AntDied;
import com.example.antfarm.ants.AntService;
import com.example.antfarm.colony.AntHatched;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.EggLaid;
import com.example.antfarm.colony.FoodDeposited;
import com.example.antfarm.food.FoodService;
import com.example.antfarm.food.FoodSourceSpawned;
import com.example.antfarm.predators.BirdAttacked;
import com.example.antfarm.predators.PredatorService;
import com.example.antfarm.simulation.internal.SimulationBroadcaster;
import com.example.antfarm.simulation.internal.SimulationProperties;
import com.example.antfarm.simulation.internal.SimulationSnapshotBuilder;
import com.example.antfarm.world.WorldService;

/**
 * Runs the full engine (world + food + predators + colony + ants) headless
 * for many ticks and asserts the simulation is alive, emits domain events,
 * and is deterministic for a fixed seed.
 */
class SimulationLifecycleTest {

	private static final int TICKS = 900;

	private SimulationProperties properties() {
		return new SimulationProperties(
				100, 20260902L,
				new SimulationProperties.World(60, 40, 8, 5),
				new SimulationProperties.Colony(200, 5, 25, 6, 10, 6, 4),
				new SimulationProperties.Ant(100, 0.2, 0.05, 35, 60, 90, 15, 40, 120,
						15, 4, 0.02, 0.6),
				new SimulationProperties.Food(4, 10, 20, 30),
				new SimulationProperties.Predators(1, 80, 7, 2));
	}

	private RunSummary run() {
		RecordingPublisher publisher = new RecordingPublisher();
		WorldService world = new WorldService();
		ColonyService colony = new ColonyService(publisher);
		FoodService food = new FoodService(world, publisher);
		PredatorService predators = new PredatorService(world, publisher);
		AntService ants = new AntService(world, colony, food, publisher);

		SimulationEngine engine = new SimulationEngine(world, colony, ants, food, predators,
				properties(), new SimulationBroadcaster(),
				new SimulationSnapshotBuilder(world, colony, ants, food, predators));
		engine.start();
		for (int i = 0; i < TICKS; i++) {
			engine.runTick();
		}

		long eggLaids = publisher.published.stream().filter(e -> e instanceof EggLaid).count();
		long hatched = publisher.published.stream().filter(e -> e instanceof AntHatched).count();
		long foodSpawned = publisher.published.stream().filter(e -> e instanceof FoodSourceSpawned).count();
		long deposits = publisher.published.stream().filter(e -> e instanceof FoodDeposited).count();
		long deaths = publisher.published.stream().filter(e -> e instanceof AntDied).count();
		long attacks = publisher.published.stream().filter(e -> e instanceof BirdAttacked).count();

		return new RunSummary(engine.tick(), ants.aliveCount(),
				colony.foodStore(engine.colonyId()).orElse(-1.0),
				eggLaids, hatched, foodSpawned, deposits, deaths, attacks);
	}

	@Test
	void colonyEcologyThrivesAndEmitsEvents() {
		RunSummary summary = run();
		assertEquals(TICKS, summary.ticks());
		assertTrue(summary.alive() > 0, "colony must still have living ants after " + TICKS + " ticks");
		assertTrue(summary.eggLaids() > 0, "queen lays eggs when fed");
		assertTrue(summary.hatched() > 0, "brood matures into roaming ants");
		assertTrue(summary.foodSpawned() > 0, "food sources appear over time");
		assertTrue(summary.deposits() > 0, "foragers must find food and deposit it (pheromones work)");
		assertTrue(summary.attacks() >= 0, "birds may or may not strike in the window");
		assertTrue(summary.deaths() >= 0);
	}

	@Test
	void sameSeedProducesIdenticalRuns() {
		RunSummary first = run();
		RunSummary second = run();
		assertEquals(first, second, "fixed seed must make the simulation deterministic");
	}

	private record RunSummary(long ticks, int alive, double foodStore,
			long eggLaids, long hatched, long foodSpawned, long deposits, long deaths, long attacks) {
	}
}
