package com.example.antfarm.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The sand terrain of the simulation: a 2D grid of cells, plus a spatial
 * registry of the entities (ants, later food/birds) standing on it.
 *
 * This is the world module's public API. Entities are referenced by an
 * opaque {@code long} id so the world never depends on another module's
 * types (ants use {@code AntId.value()}).
 *
 * Single world instance, mutated only by the simulation engine's tick
 * thread — deliberately not thread-safe.
 */
@Service
@com.example.ddd.DDDApplicationService
public class WorldService {

	private static final Logger log = LoggerFactory.getLogger(WorldService.class);

	private int width;
	private int height;
	private TerrainKind[][] terrain;
	private Long[][] occupant;
	private float[][] foodScent;

	/** Creates and fills the world; scatters the requested obstacles randomly. */
	public void create(int width, int height, long seed, int branches, int pebbles) {
		this.width = width;
		this.height = height;
		this.terrain = new TerrainKind[width][height];
		this.occupant = new Long[width][height];
		this.foodScent = new float[height][width];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				terrain[x][y] = TerrainKind.SAND;
			}
		}
		Random random = new Random(seed);
		int placed = scatter(random, TerrainKind.BRANCH, branches)
				+ scatter(random, TerrainKind.PEBBLE, pebbles);
		if (placed < branches + pebbles) {
			log.warn("World generation placed only {}/{} requested obstacles (dense terrain?)", placed, branches + pebbles);
		}
		log.info("World created: {}x{} cells, {} obstacles ({} branches, {} pebbles)",
				width, height, placed, branches, pebbles);
	}

	private int scatter(Random random, TerrainKind kind, int count) {
		int placed = 0;
		for (int attempt = 0; attempt < count * 30 && placed < count; attempt++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			if (terrain[x][y] == TerrainKind.SAND) {
				terrain[x][y] = kind;
				placed++;
			}
		}
		return placed;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public boolean inBounds(Position p) {
		return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
	}

	public TerrainKind terrainAt(Position p) {
		if (!inBounds(p)) {
			throw new IllegalArgumentException("Out of bounds: " + p);
		}
		return terrain[p.x()][p.y()];
	}

	public boolean isWalkable(Position p) {
		return inBounds(p) && terrain[p.x()][p.y()].isWalkable();
	}

	/** Walkable and not currently occupied by another entity. */
	public boolean isFree(Position p) {
		return inBounds(p) && terrain[p.x()][p.y()].isWalkable() && occupant[p.x()][p.y()] == null;
	}

	/** Digs sand into a nest chamber (workers). Fails on obstacles/occupants. */
	public boolean dig(Position p) {
		if (!inBounds(p) || terrain[p.x()][p.y()] != TerrainKind.SAND || occupant[p.x()][p.y()] != null) {
			log.debug("Dig rejected at {}", p);
			return false;
		}
		terrain[p.x()][p.y()] = TerrainKind.CHAMBER;
		log.debug("Chamber dug at {}", p);
		return true;
	}

	// ------------------------------------------------------------------
	// Entity registry (occupancy)
	// ------------------------------------------------------------------

	/** Registers an entity; returns false if the cell is not free. */
	public boolean register(long id, Position p) {
		if (!isFree(p)) {
			log.warn("Cannot register entity {} at {}: cell not free", id, p);
			return false;
		}
		occupant[p.x()][p.y()] = id;
		return true;
	}

	public boolean move(long id, Position from, Position to) {
		if (!inBounds(from) || occupant[from.x()][from.y()] == null
				|| occupant[from.x()][from.y()] != id) {
			log.warn("Move rejected: entity {} not at {} (occupied by {})", id, from, occupantAt(from).orElse(-1L));
			return false;
		}
		if (!isFree(to)) {
			log.debug("Move blocked: {} -> {} not free", from, to);
			return false;
		}
		occupant[from.x()][from.y()] = null;
		occupant[to.x()][to.y()] = id;
		return true;
	}

	public void unregister(long id, Position p) {
		if (inBounds(p) && occupant[p.x()][p.y()] != null && occupant[p.x()][p.y()] == id) {
			occupant[p.x()][p.y()] = null;
		} else {
			log.warn("Unregister mismatch for {} at {}", id, p);
		}
	}

	public OptionalLong occupantAt(Position p) {
		if (!inBounds(p) || occupant[p.x()][p.y()] == null) {
			return OptionalLong.empty();
		}
		return OptionalLong.of(occupant[p.x()][p.y()]);
	}

	// ------------------------------------------------------------------
	// Spatial queries used by behaviour
	// ------------------------------------------------------------------

	/** Free, walkable orthogonal neighbours of a cell (for movement). */
	public List<Position> freeNeighbours(Position p) {
		List<Position> result = new ArrayList<>(4);
		for (Position n : p.neighbours()) {
			if (isFree(n)) {
				result.add(n);
			}
		}
		return result;
	}

	/** Random free cell within a Manhattan-ish box around {@code centre} (inclusive). */
	public Optional<Position> freeCellNear(Position centre, int radius, Random random) {
		for (int attempt = 0; attempt < 100; attempt++) {
			int nx = centre.x() + random.nextInt(radius * 2 + 1) - radius;
			int ny = centre.y() + random.nextInt(radius * 2 + 1) - radius;
			if (nx < 0 || ny < 0) {
				continue; // would fall outside the world
			}
			Position candidate = new Position(nx, ny);
			if (isFree(candidate)) {
				return Optional.of(candidate);
			}
		}
		log.debug("No free cell found near {} within radius {}", centre, radius);
		return Optional.empty();
	}

	/** Random free sand cell anywhere (nest placement, later food spawning). */
	public Optional<Position> randomFreeSand(Random random) {
		for (int attempt = 0; attempt < 500; attempt++) {
			Position candidate = new Position(random.nextInt(width), random.nextInt(height));
			if (terrain[candidate.x()][candidate.y()] == TerrainKind.SAND && occupant[candidate.x()][candidate.y()] == null) {
				return Optional.of(candidate);
			}
		}
		log.error("No free sand cell found after 500 attempts on {}x{} world", width, height);
		return Optional.empty();
	}

	public int countObstacles() {
		int count = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (terrain[x][y] != TerrainKind.SAND && terrain[x][y] != TerrainKind.CHAMBER) {
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Read-only copy of the terrain for rendering: {@code [y][x]} cell kind
	 * ordinals ({@link TerrainKind#ordinal()}), one row of the grid per row.
	 */
	public int[][] terrainRows() {
		int[][] rows = new int[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				rows[y][x] = terrain[x][y].ordinal();
			}
		}
		return rows;
	}

	// ------------------------------------------------------------------
	// Pheromone (food scent) field
	// ------------------------------------------------------------------

	private static final float MAX_SCENT = 2.5f; // heaviness accumulates with repeated traffic

	/** Adds food scent to a cell (food sources and laden foragers emit it). */
	public void emitFoodScent(Position p, float amount) {
		if (inBounds(p)) {
			float current = foodScent[p.y()][p.x()];
			foodScent[p.y()][p.x()] = Math.min(MAX_SCENT, current + amount);
		}
	}

	public float foodScentAt(Position p) {
		return inBounds(p) ? foodScent[p.y()][p.x()] : 0f;
	}

	/**
	 * One tick of scent physics: very gentle evaporation plus mild diffusion,
	 * so pheromones linger for a long time (many seconds to minutes) while a
	 * faint gradient still points toward heavy concentrations. Heaviness
	 * accumulates up to {@link #MAX_SCENT} with repeated traffic.
	 */
	public void advanceScent() {
		float[][] next = new float[height][width];
		float keep = 0.998f;   // evaporation: trails fade over ~minutes, not ticks
		float spread = 0.25f;  // mild diffusion toward neighbours
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float sum = 0;
				int n = 0;
				if (x > 0) { sum += foodScent[y][x - 1]; n++; }
				if (x + 1 < width) { sum += foodScent[y][x + 1]; n++; }
				if (y > 0) { sum += foodScent[y - 1][x]; n++; }
				if (y + 1 < height) { sum += foodScent[y + 1][x]; n++; }
				float neighbourMean = n > 0 ? sum / n : 0f;
				next[y][x] = (foodScent[y][x] * keep) * (1f - spread) + neighbourMean * spread;
			}
		}
		foodScent = next;
	}

	// ------------------------------------------------------------------
	// Entity queries for predators
	// ------------------------------------------------------------------

	/** Ids of all entities (currently: ants) within a square radius of a cell. */
	public java.util.List<Long> occupantIdsNear(Position centre, int radius) {
		java.util.List<Long> found = new ArrayList<>();
		for (int y = Math.max(0, centre.y() - radius); y <= Math.min(height - 1, centre.y() + radius); y++) {
			for (int x = Math.max(0, centre.x() - radius); x <= Math.min(width - 1, centre.x() + radius); x++) {
				if (occupant[x][y] != null) {
					found.add(occupant[x][y]);
				}
			}
		}
		return found;
	}
}
