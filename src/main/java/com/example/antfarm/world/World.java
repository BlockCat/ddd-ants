package com.example.antfarm.world;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

/**
 * The world's public API — the only service contract the other modules are
 * allowed to use from this module.
 *
 * <p>The world is the spatial substrate of the simulation: a 2D grid of
 * terrain cells, a registry of the entities standing on it, and the
 * pheromone (food scent) field ants read and write. It behaves like an
 * in-process spatial database: consumers call these methods synchronously
 * for reads (queries) and writes (movement, digging, scent), while business
 * facts still travel between the <em>domain</em> contexts as events.
 *
 * <p>There is exactly one implementation, {@code world.internal.WorldService};
 * it is internal so consumers program against this interface, never against
 * the concrete type. The only other public types of this module are the
 * value objects used in this interface's signatures: {@link Position} and
 * {@link TerrainKind} (the world's published language).
 */
public interface World {

	/** Creates and fills the world; scatters the requested obstacles randomly. */
	void create(int width, int height, long seed, int branches, int pebbles);

	int width();

	int height();

	boolean inBounds(Position p);

	TerrainKind terrainAt(Position p);

	boolean isWalkable(Position p);

	boolean isUnderground(Position p);

	/** Walkable and not currently occupied by another entity. */
	boolean isFree(Position p);

	/**
	 * Establishes the colony's starting nest at {@code entrance}: turns the
	 * entrance cell into a {@code HOLE} and carves a small starter burrow
	 * (tunnels + chambers) around it.
	 */
	void establishNest(Position entrance, Random random);

	/** Digs a nest chamber out of sand (workers). Connected growth only. */
	boolean digChamber(Position p);

	/** Digs a tunnel corridor out of sand (workers). Connected growth only. */
	boolean digTunnel(Position p);

	/** Opens a burrow exit on the surface (workers). */
	boolean openHole(Position p);

	/** Whether the cell borders the burrow network (underground cell or a hole). */
	boolean canConnectToBurrow(Position p);

	/** Whether the cell borders an underground cell (tunnel/chamber). */
	boolean canConnectUnderground(Position p);

	/** Legacy alias: a worker digging a chamber. */
	boolean dig(Position p);

	/** Registers an entity; returns false if the cell is not free. */
	boolean register(long id, Position p);

	/** Whether an ant may move between two adjacent cells (surface/underground rules). */
	boolean movementAllowed(TerrainKind from, TerrainKind to);

	boolean move(long id, Position from, Position to);

	void unregister(long id, Position p);

	OptionalLong occupantAt(Position p);

	/** Free, walkable orthogonal neighbours respecting the surface/underground rules. */
	List<Position> movementNeighbours(Position p);

	/** Free sand neighbours of a cell (digging frontier). */
	List<Position> sandNeighbours(Position p);

	/** Free surface cells (sand or holes) near a centre. */
	Optional<Position> freeSurfaceCellNear(Position centre, int radius, Random random);

	/** Free underground cells (tunnels/chambers) near a centre. */
	Optional<Position> freeUndergroundCellNear(Position centre, int radius, Random random);

	/** Any free walkable cell near a centre (generic placement helper). */
	Optional<Position> freeCellNear(Position centre, int radius, Random random);

	/** Random free sand cell anywhere (nest placement, food spawning). */
	Optional<Position> randomFreeSand(Random random);

	int countObstacles();

	/**
	 * Read-only copy of the terrain for rendering: {@code [y][x]} cell kind
	 * ordinals ({@link TerrainKind#ordinal()}), one row of the grid per row.
	 */
	int[][] terrainRows();

	/** Adds food scent to a cell (food sources and laden foragers emit it). */
	void emitFoodScent(Position p, float amount);

	float foodScentAt(Position p);

	/** One tick of scent physics: evaporation + diffusion. */
	void advanceScent();

	/** Ids of all entities (currently: ants) within a square radius of a cell. */
	List<Long> occupantIdsNear(Position centre, int radius);
}
