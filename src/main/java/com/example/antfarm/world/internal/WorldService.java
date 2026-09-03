package com.example.antfarm.world.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.antfarm.world.Position;
import com.example.antfarm.world.TerrainKind;
import com.example.antfarm.world.World;

/**
 * The ant world: a 2D grid of terrain cells plus a spatial registry of the
 * entities standing on it.
 *
 * <p>The world module owns the surface (sand, branches, pebbles) and the
 * <b>burrow</b> — the underground network of tunnels and chambers the
 * colony carves out of the sand, with {@code HOLE} entrances/exits that
 * connect it to the surface. Terrain changes only through the domain
 * operations here ({@link #establishNest}, {@link #digChamber},
 * {@link #digTunnel}, {@link #openHole}), so the world itself behaves like a
 * mutable entity whose state the colony shapes over time.
 *
 * <p>Entities are referenced by an opaque {@code long} id so the world never
 * depends on another module's types (ants use {@code AntId.value()}).
 *
 * <p>Single world instance, mutated only by the simulation engine's tick
 * thread — deliberately not thread-safe.
 */
@Service
@com.example.ddd.DDDApplicationService
public class WorldService implements World {

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

	public boolean isUnderground(Position p) {
		return inBounds(p) && terrain[p.x()][p.y()].isUnderground();
	}

	/** Walkable and not currently occupied by another entity. */
	public boolean isFree(Position p) {
		return inBounds(p) && terrain[p.x()][p.y()].isWalkable() && occupant[p.x()][p.y()] == null;
	}

	// ------------------------------------------------------------------
	// Burrow — the nest network carved out of the sand (world as entity)
	// ------------------------------------------------------------------

	/**
	 * Establishes the colony's starting nest at {@code entrance}: turns the
	 * entrance cell into a {@code HOLE} and carves a small starter burrow
	 * (tunnels + chambers) around it so the nest is visible from the start.
	 */
	public void establishNest(Position entrance, Random random) {
		if (!carve(entrance, TerrainKind.HOLE)) {
			log.warn("Could not establish nest at {}", entrance);
			return;
		}
		Position t1 = carveFrom(entrance, TerrainKind.TUNNEL, random).orElse(null);
		Position t2 = t1 == null ? null : carveFrom(t1, TerrainKind.TUNNEL, random).orElse(null);
		Position c1 = t2 == null ? null : carveFrom(t2, TerrainKind.CHAMBER, random).orElse(null);
		Position c2 = c1 == null ? null : carveFrom(c1, TerrainKind.CHAMBER, random).orElse(null);
		log.info("Nest established: entrance hole at {}, tunnels {}, chambers {}",
				entrance, t1 != null ? List.of(t1, t2) : List.of(),
				c1 != null ? List.of(c1, c2) : List.of());
	}

	private Optional<Position> carveFrom(Position cell, TerrainKind kind, Random random) {
		List<Position> sand = sandNeighbours(cell);
		if (sand.isEmpty()) {
			return Optional.empty();
		}
		Position target = sand.get(random.nextInt(sand.size()));
		carve(target, kind);
		log.debug("Carved {} at {}", kind, target);
		return Optional.of(target);
	}

	/** Carves {@code p} into {@code kind} if it is still free sand. */
	private boolean carve(Position p, TerrainKind kind) {
		if (!inBounds(p) || terrain[p.x()][p.y()] != TerrainKind.SAND || occupant[p.x()][p.y()] != null) {
			return false;
		}
		terrain[p.x()][p.y()] = kind;
		return true;
	}

	/** Digs a nest chamber out of sand (workers). Only connected growth: the
	 *  target must border the burrow network (a tunnel/chamber or a hole). */
	public boolean digChamber(Position p) {
		if (!canConnectToBurrow(p)) {
			log.debug("Chamber dig rejected at {} — not connected to the burrow", p);
			return false;
		}
		boolean dug = carve(p, TerrainKind.CHAMBER);
		if (dug) {
			log.debug("Chamber dug at {}", p);
		}
		return dug;
	}

	/** Digs a tunnel corridor out of sand (workers). Connected growth only. */
	public boolean digTunnel(Position p) {
		if (!canConnectToBurrow(p)) {
			log.debug("Tunnel dig rejected at {} — not connected to the burrow", p);
			return false;
		}
		boolean dug = carve(p, TerrainKind.TUNNEL);
		if (dug) {
			log.debug("Tunnel dug at {}", p);
		}
		return dug;
	}

	/** Opens a burrow exit on the surface (workers). The hole must border an
	 *  underground cell so it always leads out of the tunnel network. */
	public boolean openHole(Position p) {
		if (!canConnectUnderground(p)) {
			log.debug("Exit rejected at {} — no tunnel to open from", p);
			return false;
		}
		boolean dug = carve(p, TerrainKind.HOLE);
		if (dug) {
			log.debug("New burrow exit opened at {}", p);
		}
		return dug;
	}

	/** Whether the cell borders the burrow network (underground cell or a hole). */
	public boolean canConnectToBurrow(Position p) {
		return hasNeighbour(p, k -> k.isUnderground() || k.isHole());
	}

	/** Whether the cell borders an underground cell (tunnel/chamber). */
	public boolean canConnectUnderground(Position p) {
		return hasNeighbour(p, TerrainKind::isUnderground);
	}

	private boolean hasNeighbour(Position p, java.util.function.Predicate<TerrainKind> test) {
		for (Position n : p.neighbours()) {
			if (inBounds(n) && test.test(terrain[n.x()][n.y()])) {
				return true;
			}
		}
		return false;
	}

	/** Legacy alias: a worker digging a chamber. */
	public boolean dig(Position p) {
		return digChamber(p);
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

	/** Whether an ant may move between two adjacent cells (surface/underground rules). */
	public boolean movementAllowed(TerrainKind from, TerrainKind to) {
		if (from.isUnderground() == to.isUnderground()) {
			return true; // both on the surface or both in the burrow
		}
		// crossing the surface/underground boundary is only possible through a hole
		return from.isHole() || to.isHole();
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
		if (!movementAllowed(terrain[from.x()][from.y()], terrain[to.x()][to.y()])) {
			log.debug("Move blocked by terrain rule: {} ({}) -> {} ({})",
					from, terrain[from.x()][from.y()], to, terrain[to.x()][to.y()]);
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

	/** Free, walkable orthogonal neighbours respecting the surface/underground rules. */
	public List<Position> movementNeighbours(Position p) {
		List<Position> result = new ArrayList<>(4);
		TerrainKind from = inBounds(p) ? terrain[p.x()][p.y()] : TerrainKind.SAND;
		for (Position n : p.neighbours()) {
			if (inBounds(n) && occupant[n.x()][n.y()] == null
					&& movementAllowed(from, terrain[n.x()][n.y()])) {
				result.add(n);
			}
		}
		return result;
	}

	/** Free sand neighbours of a cell (digging frontier). */
	public List<Position> sandNeighbours(Position p) {
		List<Position> result = new ArrayList<>(4);
		for (Position n : p.neighbours()) {
			if (inBounds(n) && terrain[n.x()][n.y()] == TerrainKind.SAND && occupant[n.x()][n.y()] == null) {
				result.add(n);
			}
		}
		return result;
	}

	/** Free surface cells (sand or holes) near a centre. */
	public Optional<Position> freeSurfaceCellNear(Position centre, int radius, Random random) {
		return freeCellNear(centre, radius, random,
				k -> k == TerrainKind.SAND || k == TerrainKind.HOLE);
	}

	/** Free underground cells (tunnels/chambers) near a centre. */
	public Optional<Position> freeUndergroundCellNear(Position centre, int radius, Random random) {
		return freeCellNear(centre, radius, random, TerrainKind::isUnderground);
	}

	private Optional<Position> freeCellNear(Position centre, int radius, Random random,
			java.util.function.Predicate<TerrainKind> kindFilter) {
		for (int attempt = 0; attempt < 100; attempt++) {
			int nx = centre.x() + random.nextInt(radius * 2 + 1) - radius;
			int ny = centre.y() + random.nextInt(radius * 2 + 1) - radius;
			if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
				continue;
			}
			Position candidate = new Position(nx, ny);
			if (occupant[nx][ny] == null && kindFilter.test(terrain[nx][ny])) {
				return Optional.of(candidate);
			}
		}
		log.debug("No free {} cell found near {} within radius {}", "filtered", centre, radius);
		return Optional.empty();
	}

	/** Any free walkable cell near a centre (generic placement helper). */
	public Optional<Position> freeCellNear(Position centre, int radius, Random random) {
		return freeCellNear(centre, radius, random, TerrainKind::isWalkable);
	}

	/** Random free sand cell anywhere (nest placement, food spawning). */
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
				TerrainKind kind = terrain[x][y];
				if (kind == TerrainKind.BRANCH || kind == TerrainKind.PEBBLE) {
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
