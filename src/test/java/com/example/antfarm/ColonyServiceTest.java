package com.example.antfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyPolicy;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.EggLaid;
import com.example.antfarm.colony.AntHatched;
import com.example.antfarm.colony.HatchRequest;
import com.example.antfarm.world.Position;

class ColonyServiceTest {

	@Test
	void queenLaysEggsOnlyWhileTheStoreCanAffordThem() {
		RecordingPublisher publisher = new RecordingPublisher();
		ColonyService colony = new ColonyService(publisher);
		ColonyId id = colony.createColony(new Position(10, 10), 100,
				new ColonyPolicy(5, 10, 4, 3)); // egg cost 5, hatch after 10 ticks, cap 4, cooldown 3

		int eggLaids = 0;
		int antHatched = 0;
		int hatchRequests = 0;
		for (int tick = 1; tick <= 40; tick++) {
			List<HatchRequest> hatches = colony.advance(tick);
			hatchRequests += hatches.size();
		}
		for (Object event : publisher.published) {
			if (event instanceof EggLaid) {
				eggLaids++;
			}
			if (event instanceof AntHatched) {
				antHatched++;
			}
		}

		// laying every 3 ticks until the brood cap of 4 is hit at tick 10, then
		// steady-state as hatches free brood slots
		assertTrue(eggLaids >= 4, "queen should lay while fed and under brood cap, laid " + eggLaids);
		assertTrue(antHatched >= 1, "first egg (tick 1) must mature at tick 11");
		assertEquals(hatchRequests, antHatched);
		assertTrue(colony.foodStore(id).orElseThrow() < 100, "egg laying consumes store food");
		assertTrue(colony.broodCount(id).orElseThrow() <= 4);
	}

	@Test
	void storeServesMealsOnlyWhileNonEmpty() {
		RecordingPublisher publisher = new RecordingPublisher();
		ColonyService colony = new ColonyService(publisher);
		ColonyId id = colony.createColony(new Position(5, 5), 100, new ColonyPolicy(5, 10, 4, 3));

		assertTrue(colony.tryConsumeFood(id, 100));
		assertEquals(0.0, colony.foodStore(id).orElseThrow(), 1e-9);
		assertFalse(colony.tryConsumeFood(id, 1), "empty store cannot serve meals");

		colony.depositFood(id, 99L, 50, 1L);
		assertEquals(50.0, colony.foodStore(id).orElseThrow(), 1e-9);
		assertTrue(colony.tryConsumeFood(id, 50));
		assertEquals(0.0, colony.foodStore(id).orElseThrow(), 1e-9);
	}
}
