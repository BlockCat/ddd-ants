package com.example.antfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.antfarm.world.Position;
import com.example.antfarm.world.TerrainKind;
import com.example.antfarm.world.internal.WorldService;

class WorldServiceTest {

	private WorldService world;

	@BeforeEach
	void setUp() {
		world = new WorldService();
		world.create(40, 30, 7, 0, 0); // obstacle-free canvas for geometry tests
	}

	@Test
	void worldHasExpectedDimensions() {
		assertEquals(40, world.width());
		assertEquals(30, world.height());
		assertTrue(world.inBounds(new Position(0, 0)));
		assertTrue(world.inBounds(new Position(39, 29)));
		assertFalse(world.inBounds(new Position(40, 0)));
		assertFalse(world.inBounds(new Position(0, 30)));
	}

	@Test
	void obstaclesAreNotWalkable() {
		world.create(40, 30, 7, 6, 4); // dedicated world with obstacles
		Position obstacle = findFirstObstacle().orElseThrow();
		assertFalse(world.isWalkable(obstacle));
		assertTrue(world.terrainAt(obstacle) == TerrainKind.BRANCH || world.terrainAt(obstacle) == TerrainKind.PEBBLE);
		assertFalse(world.isFree(obstacle));
		assertFalse(world.register(99, obstacle), "cannot register on an obstacle");
	}

	@Test
	void registerMoveAndUnregisterMaintainOccupancy() {
		Position from = new Position(5, 5);
		Position to = new Position(6, 5);
		assertTrue(world.register(1, from));
		assertEquals(OptionalLong.of(1), world.occupantAt(from));
		assertFalse(world.isFree(from));
		assertFalse(world.register(2, from), "cell already occupied");

		assertTrue(world.move(1, from, to));
		assertEquals(OptionalLong.empty(), world.occupantAt(from));
		assertEquals(OptionalLong.of(1), world.occupantAt(to));
		assertFalse(world.move(1, from, to), "entity no longer at 'from'");

		world.unregister(1, to);
		assertEquals(OptionalLong.empty(), world.occupantAt(to));
	}

	@Test
	void freeNeighboursExcludesOccupiedAndBlockedCells() {
		Position centre = new Position(10, 10);
		world.register(1, centre);
		world.register(2, new Position(10, 9)); // north neighbour occupied
		List<Position> free = world.movementNeighbours(centre);
		assertEquals(3, free.size());
		assertFalse(free.contains(new Position(10, 9)));
	}

	@Test
	void freeCellNearStaysNearAndFree() {
		Random random = new Random(11);
		Position centre = new Position(20, 15);
		Optional<Position> spot = world.freeCellNear(centre, 2, random);
		assertTrue(spot.isPresent());
		assertTrue(centre.distanceTo(spot.get()) <= 4); // box radius 2
		assertTrue(world.isFree(spot.get()));
	}

	@Test
	void diggingIsAlwaysConnectedToTheBurrow() {
		// without a burrow there is nothing to dig from — no disconnected islands
		assertFalse(world.dig(new Position(3, 3)));
		assertFalse(world.digTunnel(new Position(3, 3)));
		assertFalse(world.digChamber(new Position(3, 3)));

		// the nest entrance hole is the seed of the whole burrow
		world.establishNest(new Position(5, 5), new Random(1));
		assertEquals(TerrainKind.HOLE, world.terrainAt(new Position(5, 5)));

		// sand bordering the entrance/burrow can be dug, and only once
		Position frontier = world.sandNeighbours(new Position(5, 5)).get(0);
		assertTrue(world.digTunnel(frontier));
		assertEquals(TerrainKind.TUNNEL, world.terrainAt(frontier));
		assertFalse(world.digTunnel(frontier), "cannot dig a tunnel twice");

		// a disconnected cell stays untouched
		assertFalse(world.dig(new Position(30, 20)));
		assertEquals(TerrainKind.SAND, world.terrainAt(new Position(30, 20)));
	}

	@Test
	void exitsOnlyOpenFromTunnels() {
		world.establishNest(new Position(5, 5), new Random(2));
		Position tunnel = findKind(TerrainKind.TUNNEL);
		assertTrue(tunnel != null, "nest carving should produce a tunnel");

		// open an exit from a sand cell bordering that tunnel
		Position sand = world.sandNeighbours(tunnel).stream()
				.filter(p -> !p.equals(new Position(5, 5)))
				.findFirst().orElse(null);
		assertTrue(sand != null, "tunnel should have sand neighbours left");
		assertTrue(world.openHole(sand));
		assertEquals(TerrainKind.HOLE, world.terrainAt(sand));

		// isolated sand far from any tunnel cannot become a hole
		assertFalse(world.openHole(new Position(30, 20)));
	}

	private Position findKind(TerrainKind kind) {
		for (int x = 0; x < world.width(); x++) {
			for (int y = 0; y < world.height(); y++) {
				if (world.terrainAt(new Position(x, y)) == kind) {
					return new Position(x, y);
				}
			}
		}
		return null;
	}

	@Test
	void randomFreeSandReturnsFreeSand() {
		Optional<Position> spot = world.randomFreeSand(new Random(3));
		assertTrue(spot.isPresent());
		assertEquals(TerrainKind.SAND, world.terrainAt(spot.get()));
		assertTrue(world.isFree(spot.get()));
	}

	private Optional<Position> findFirstObstacle() {
		for (int x = 0; x < world.width(); x++) {
			for (int y = 0; y < world.height(); y++) {
				Position p = new Position(x, y);
				TerrainKind kind = world.terrainAt(p);
				if (kind == TerrainKind.BRANCH || kind == TerrainKind.PEBBLE) {
					return Optional.of(p);
				}
			}
		}
		return Optional.empty();
	}
}
