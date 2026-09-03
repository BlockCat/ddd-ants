package com.example.antfarm.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.antfarm.world.World;

/**
 * Module-scoped test for the world context: boots only the world module
 * (STANDALONE) and exercises its public {@code WorldService} API.
 */
@ActiveProfiles("test")
@ApplicationModuleTest
class WorldModuleTests {

	@Autowired
	World world;

	@Test
	void createsGridAndTracksOccupancy() {
		world.create(40, 30, 7, 0, 0);

		assertEquals(40, world.width());
		assertEquals(30, world.height());

		Position from = new Position(5, 5);
		Position to = new Position(6, 5);

		assertTrue(world.register(1, from));
		assertEquals(OptionalLong.of(1), world.occupantAt(from));
		assertFalse(world.register(2, from), "occupied cell cannot be registered twice");

		assertTrue(world.move(1, from, to));
		assertEquals(OptionalLong.empty(), world.occupantAt(from));
		assertEquals(OptionalLong.of(1), world.occupantAt(to));

		world.unregister(1, to);
		assertEquals(OptionalLong.empty(), world.occupantAt(to));
	}

	@Test
	void carvesNestAndDigsConnectedBurrow() {
		world.create(40, 30, 7, 0, 0);
		Position entrance = new Position(5, 5);

		world.establishNest(entrance, new Random(1));
		assertEquals(TerrainKind.HOLE, world.terrainAt(entrance));

		Position frontier = world.sandNeighbours(entrance).get(0);
		assertTrue(world.digTunnel(frontier));
		assertEquals(TerrainKind.TUNNEL, world.terrainAt(frontier));

		// an isolated sand cell far from the burrow cannot be dug
		assertFalse(world.digChamber(new Position(30, 20)));
	}
}
