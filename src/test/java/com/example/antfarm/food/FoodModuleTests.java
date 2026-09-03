package com.example.antfarm.food;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.antfarm.world.WorldService;

/**
 * Module-scoped test for the food context: boots the food module together
 * with its direct dependency (world) and asserts that advancing the ecology
 * spawns a source and publishes {@code FoodSourceSpawned}.
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
class FoodModuleTests {

	@Autowired
	FoodService food;

	@Autowired
	WorldService world;

	@Test
	void spawnsSourceAndPublishesFoodSourceSpawned(Scenario scenario) {
		world.create(40, 30, 7, 0, 0);
		food.configure(6, 10, 20, 30, 1);

		// first spawn window opens at tick 10
		scenario.stimulate(() -> food.advance(10))
				.andWaitForEventOfType(FoodSourceSpawned.class)
				.toArrive();

		assertEquals(1, food.sourceCount());
	}
}
