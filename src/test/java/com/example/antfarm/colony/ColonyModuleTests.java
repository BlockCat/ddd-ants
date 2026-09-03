package com.example.antfarm.colony;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import com.example.antfarm.world.Position;

/**
 * Module-scoped test for the colony context: boots only the colony module
 * (STANDALONE) and exercises its public {@code ColonyService} API, using the
 * Modulith Scenario API to assert that the lifecycle facts are published and
 * delivered.
 */
@ApplicationModuleTest
class ColonyModuleTests {

	@Autowired
	ColonyService colony;

	@Test
	void queenLaysEggsAndPublishesEggLaid(Scenario scenario) {
		colony.createColony(new Position(5, 5), 100, new ColonyPolicy(5, 10, 4, 3));

		scenario.stimulate(() -> colony.advance(1))
				.andWaitForEventOfType(EggLaid.class)
				.toArrive();
	}

	@Test
	void broodMaturesIntoHatchRequestAndPublishesAntHatched(Scenario scenario) {
		// eggTicks=2, cooldown=0: egg laid on tick 1 matures on tick 2
		colony.createColony(new Position(5, 5), 100, new ColonyPolicy(5, 2, 4, 0));

		scenario.stimulate(() -> colony.advance(1)).andWaitForEventOfType(EggLaid.class).toArrive();
		scenario.stimulate(() -> colony.advance(2))
				.andWaitForEventOfType(AntHatched.class)
				.toArrive();
	}

	@Test
	void storeServesMealsAndRecordsDeposits(Scenario scenario) {
		ColonyId id = colony.createColony(new Position(5, 5), 100, new ColonyPolicy(5, 10, 4, 3));

		assertTrue(colony.tryConsumeFood(id, 30));
		assertEquals(70.0, colony.foodStore(id).orElseThrow(), 1e-9);

		scenario.stimulate(() -> colony.depositFood(id, 42L, 25, 1))
				.andWaitForEventOfType(FoodDeposited.class)
				.toArrive();
		assertEquals(95.0, colony.foodStore(id).orElseThrow(), 1e-9);
	}
}
