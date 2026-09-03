package com.example.antfarm.simulation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.antfarm.ants.AntService;
import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.Role;
import com.example.antfarm.food.FoodService;
import com.example.antfarm.predators.PredatorService;
import com.example.antfarm.world.WorldService;

/**
 * Assembles {@link SimulationSnapshot}s from the module public APIs. Used by
 * the engine (per tick, when clients watch) and by the REST endpoints.
 */
@Component
public class SimulationSnapshotBuilder {

	private final WorldService world;
	private final ColonyService colony;
	private final AntService ants;
	private final FoodService food;
	private final PredatorService predators;

	public SimulationSnapshotBuilder(WorldService world, ColonyService colony, AntService ants,
			FoodService food, PredatorService predators) {
		this.world = world;
		this.colony = colony;
		this.ants = ants;
		this.food = food;
		this.predators = predators;
	}

	public SimulationSnapshot current(long tick, ColonyId colonyId, boolean running, double ticksPerSecond) {
		int width = world.width();
		int height = world.height();
		if (colonyId == null) {
			return SimulationSnapshot.empty(tick, width, height);
		}

		List<SimulationSnapshot.Ant> roaming = ants.roaming().stream()
				.map(a -> new SimulationSnapshot.Ant(a.id().value(), a.role().name(),
						a.position().x(), a.position().y(), a.energy(), a.carrying()))
				.toList();

		List<SimulationSnapshot.Nest> nests = colony.entranceOf(colonyId)
				.map(p -> List.of(new SimulationSnapshot.Nest(colonyId.value(), p.x(), p.y())))
				.orElseGet(List::of);

		List<SimulationSnapshot.Food> foods = food.all().stream()
				.map(f -> new SimulationSnapshot.Food(f.id(), f.position().x(), f.position().y(), f.amount()))
				.toList();

		List<SimulationSnapshot.Bird> birds = predators.all().stream()
				.map(b -> new SimulationSnapshot.Bird(b.id(), b.position().x(), b.position().y()))
				.toList();

		return new SimulationSnapshot(tick, running, ticksPerSecond, width, height,
				ants.aliveCount(),
				(int) ants.countByRole(Role.WORKER),
				(int) ants.countByRole(Role.FORAGER),
				colony.foodStore(colonyId).orElse(0.0),
				colony.broodCount(colonyId).orElse(0),
				food.sourceCount(),
				predators.birdCount(),
				roaming, nests, foods, birds);
	}
}
