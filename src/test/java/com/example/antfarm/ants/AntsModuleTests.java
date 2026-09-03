package com.example.antfarm.ants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import com.example.antfarm.colony.AntHatched;
import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.Role;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.World;

/**
 * Module-scoped test for the ants context: boots the ants module with its
 * direct dependencies (world) and asserts both the ant life-cycle commands
 * and the event-driven cross-module effects ({@code AntHatched} spawns an
 * ant). Bird strikes are not tested here — the simulation context mediates
 * them into {@code AntService.kill}, so the ants context never depends on
 * predators.
 */
@ActiveProfiles("test")
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class AntsModuleTests {

	@Autowired
	AntService ants;

	@Autowired
	World world;

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

		events.publishEvent(new AntHatched(new ColonyId(1), Role.FORAGER, new Position(5, 5), 1));

		assertEquals(1, ants.aliveCount(), "AntHatched must be consumed and spawn a roaming ant");
	}
}
