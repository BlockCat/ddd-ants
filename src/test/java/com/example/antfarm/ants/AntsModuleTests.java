package com.example.antfarm.ants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyPolicy;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.Role;
import com.example.antfarm.predators.BirdAttacked;
import com.example.antfarm.predators.BirdId;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.WorldService;

/**
 * Module-scoped test for the ants context: boots the ants module with its
 * direct dependencies (world, colony, food, predators) and asserts both the
 * ant life-cycle commands and the event-driven cross-module effects
 * ({@code AntHatched} spawns an ant, {@code BirdAttacked} kills one).
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class AntsModuleTests {

	@Autowired
	AntService ants;

	@Autowired
	WorldService world;

	@Autowired
	ColonyService colony;

	@Autowired
	ApplicationEventPublisher events;

	@Test
	void spawnsAntAndPublishesAntDiedWhenKilled(Scenario scenario) {
		world.create(40, 30, 7, 0, 0);
		ants.configure(AntPolicy.DEFAULTS, 1);

		ColonyId colonyId = new ColonyId(1);
		Position entrance = new Position(5, 5);
		AntId id = ants.spawn(new SpawnAnt(colonyId, Role.FORAGER, entrance));

		assertEquals(1, ants.aliveCount());

		scenario.stimulate(() -> ants.kill(id, AntDeathCause.EATEN, 1))
				.andWaitForEventOfType(AntDied.class)
				.toArrive();

		assertEquals(0, ants.aliveCount());
	}

	@Test
	void reactsToAntHatchedBySpawningTheAdult() {
		world.create(40, 30, 7, 0, 0);
		ants.configure(AntPolicy.DEFAULTS, 1);

		// eggTicks=1: the egg laid on tick 1 matures within the same advance()
		colony.createColony(new Position(5, 5), 100, new ColonyPolicy(5, 1, 4, 0));
		colony.advance(1);

		assertEquals(1, ants.aliveCount(), "AntHatched must be consumed and spawn a roaming ant");
	}

	@Test
	void reactsToBirdAttackedByKillingTheVictim() {
		world.create(40, 30, 7, 0, 0);
		ants.configure(AntPolicy.DEFAULTS, 1);

		AntId victim = ants.spawn(new SpawnAnt(new ColonyId(1), Role.FORAGER, new Position(5, 5)));
		assertEquals(1, ants.aliveCount());

		events.publishEvent(new BirdAttacked(new BirdId(1), victim.value(), new Position(5, 5), 1));

		assertEquals(0, ants.aliveCount(), "BirdAttacked must be consumed and kill the ant");
	}
}
