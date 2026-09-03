package com.example.antfarm.predators;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.antfarm.world.WorldService;

/**
 * Module-scoped test for the predators context: boots the predators module
 * with its direct dependency (world) and asserts that a bird striking an ant
 * on open sand announces {@code BirdAttacked} via an event (the ants context
 * reacts to it).
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class PredatorModuleTests {

	@Autowired
	PredatorService predators;

	@Autowired
	WorldService world;

	@Test
	void birdStrikesAntAndPublishesBirdAttacked(Scenario scenario) {
		world.create(40, 30, 7, 0, 0);
		predators.configure(1, 10, 8, 2, 1);

		PredatorService.BirdLocation bird = predators.all().get(0);
		assertTrue(world.register(99, bird.position()), "victim ant should stand on open sand");

		// tick 11 is odd (no drift with moveEveryTicks=2) and past the first hunt delay (0..9)
		scenario.stimulate(() -> predators.advance(11))
				.andWaitForEventOfType(BirdAttacked.class)
				.toArrive();
	}
}
