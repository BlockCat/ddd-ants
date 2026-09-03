package com.example.antfarm.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.antfarm.ants.AntDeathCause;
import com.example.antfarm.ants.AntId;
import com.example.antfarm.ants.AntPolicy;
import com.example.antfarm.ants.AntService;
import com.example.antfarm.ants.SpawnAnt;
import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyPolicy;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.Role;
import com.example.antfarm.food.FoodService;
import com.example.antfarm.predators.BirdAttacked;
import com.example.antfarm.predators.PredatorService;
import com.example.antfarm.simulation.internal.SimulationBroadcaster;
import com.example.antfarm.simulation.internal.SimulationProperties;
import com.example.antfarm.simulation.internal.SimulationSnapshotBuilder;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.World;

import jakarta.annotation.PostConstruct;

/**
 * The tick engine. Runs on a single scheduled thread, advancing the
 * contexts in a fixed order each tick so the simulation is deterministic:
 *
 * <ol>
 *   <li>world — scent physics (evaporate &amp; diffuse pheromones)</li>
 *   <li>food — spawn sources, emit food scent from every source</li>
 *   <li>predators — birds drift and hunt; a strike publishes
 *       {@code BirdAttacked} and the ants context kills the victim</li>
 *   <li>colony — queen may lay, brood matures; a hatch publishes
 *       {@code AntHatched} and the ants context spawns the adult</li>
 *   <li>ants — every ant acts (forage, dig, carry, deposit, feed, die)</li>
 * </ol>
 *
 * The tick is transactional: significant domain events published along the
 * way are written to the Modulith event-publication outbox and delivered to
 * async listeners after commit. Live simulation state itself is in-memory.
 *
 * Controls: {@link #pause()} / {@link #resume()} and a speed multiplier
 * (ticks executed per scheduler wake-up).
 */
@Service
@com.example.ddd.DDDApplicationService
public class SimulationEngine {

	private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

	private final World world;
	private final ColonyService colony;
	private final AntService ants;
	private final FoodService food;
	private final PredatorService predators;
	private final SimulationProperties properties;
	private final SimulationBroadcaster broadcaster;
	private final SimulationSnapshotBuilder snapshots;

	private ColonyId colonyId;
	private final java.util.concurrent.atomic.AtomicLong tick = new java.util.concurrent.atomic.AtomicLong();

	private volatile boolean paused;
	private double speed = 1.0;
	private double speedAccumulator;

	public SimulationEngine(World world, ColonyService colony, AntService ants, FoodService food,
			PredatorService predators, SimulationProperties properties, SimulationBroadcaster broadcaster,
			SimulationSnapshotBuilder snapshots) {
		this.world = world;
		this.colony = colony;
		this.ants = ants;
		this.food = food;
		this.predators = predators;
		this.properties = properties;
		this.broadcaster = broadcaster;
		this.snapshots = snapshots;
	}

	@PostConstruct
	public void start() {
		if (colonyId != null) {
			return;
		}
		log.info("Starting simulation with {}", properties);
		java.util.Random random = new java.util.Random(properties.randomSeed());
		try {
			SimulationProperties.World worldCfg = properties.world();
			world.create(worldCfg.width(), worldCfg.height(), properties.randomSeed(),
					worldCfg.branches(), worldCfg.pebbles());

			Position entrance = world.randomFreeSand(random)
					.orElseThrow(() -> new IllegalStateException(
							"No free sand cell for the nest — world too small for the obstacle count?"));
			world.establishNest(entrance, random);

			SimulationProperties.Colony colonyCfg = properties.colony();
			colonyId = colony.createColony(entrance, colonyCfg.initialFood(),
					new ColonyPolicy(colonyCfg.eggCost(), colonyCfg.eggTicks(), colonyCfg.broodCap(),
							colonyCfg.layCooldownTicks()));

			SimulationProperties.Ant antCfg = properties.ant();
			ants.configure(new AntPolicy(antCfg.startEnergy(), antCfg.outsideCost(), antCfg.insideCost(),
					antCfg.eatThreshold(), antCfg.mealAmount(), antCfg.leaveThreshold(),
					antCfg.leaveIntervalTicks(), antCfg.minExploreTicks(), antCfg.maxExploreTicks(),
					antCfg.carryingCapacity(), antCfg.sniffRadius(), antCfg.digProbability(), antCfg.digCost()),
					properties.randomSeed() + 1);

			SimulationProperties.Food foodCfg = properties.food();
			food.configure(foodCfg.maxSources(), foodCfg.spawnIntervalTicks(),
					foodCfg.minAmount(), foodCfg.maxAmount(), properties.randomSeed() + 2);

			SimulationProperties.Predators predatorCfg = properties.predators();
			predators.configure(predatorCfg.birdCount(), predatorCfg.huntIntervalTicks(),
					predatorCfg.huntRadius(), predatorCfg.moveEveryTicks(), properties.randomSeed() + 3);

			for (int i = 0; i < colonyCfg.initialWorkers(); i++) {
				ants.spawn(new SpawnAnt(colonyId, Role.WORKER, entrance));
			}
			for (int i = 0; i < colonyCfg.initialForagers(); i++) {
				ants.spawn(new SpawnAnt(colonyId, Role.FORAGER, entrance));
			}
			log.info("Simulation ready: colony {} at {} with {} food, {} workers and {} foragers seeded",
					colonyId, entrance, colonyCfg.initialFood(),
					colonyCfg.initialWorkers(), colonyCfg.initialForagers());
		} catch (RuntimeException ex) {
			log.error("Simulation failed to start: {}", ex.getMessage(), ex);
			throw ex;
		}
	}

	public ColonyId colonyId() {
		return colonyId;
	}

	/**
	 * Mediates a bird strike: the predators context publishes
	 * {@link BirdAttacked} as its own fact, and the engine translates it into
	 * the owning ants context's command so the victim dies there — ants never
	 * has to depend on predators.
	 */
	@EventListener
	void onBirdAttacked(BirdAttacked event) {
		ants.kill(new AntId(event.antId()), AntDeathCause.EATEN, event.tick());
	}

	public long tick() {
		return tick.get();
	}

	public boolean isPaused() {
		return paused;
	}

	public double ticksPerSecond() {
		return (1000.0 / properties.tickIntervalMs()) * speed;
	}

	public void pause() {
		this.paused = true;
		log.info("Simulation paused at tick {}", tick.get());
		pushStatus();
	}

	public void resume() {
		this.paused = false;
		log.info("Simulation resumed at tick {}", tick.get());
		pushStatus();
	}

	/** Speed multiplier (e.g. 0.5 = half speed, 2 = double). */
	public void setSpeed(double speed) {
		this.speed = Math.max(0.25, Math.min(8.0, speed));
		this.speedAccumulator = 0;
		log.info("Simulation speed set to {}x ({} ticks/s)", this.speed, Math.round(ticksPerSecond()));
		pushStatus();
	}

	/** Sends an immediate snapshot so clients see control changes right away. */
	private void pushStatus() {
		if (colonyId != null && broadcaster.hasSubscribers()) {
			broadcaster.broadcast(snapshots.current(tick.get(), colonyId, !paused, ticksPerSecond()));
		}
	}

	@Scheduled(fixedDelayString = "${simulation.tick-interval-ms}")
	@Transactional
	public void runTick() {
		if (paused || colonyId == null) {
			return;
		}
		speedAccumulator += speed;
		int ticksToRun = (int) speedAccumulator;
		speedAccumulator -= ticksToRun;
		for (int i = 0; i < ticksToRun; i++) {
			try {
				tickOnce();
			} catch (RuntimeException ex) {
				log.error("Tick {} failed: {}", tick.get(), ex.getMessage(), ex);
			}
		}
	}

	private void tickOnce() {
		long current = tick.incrementAndGet();

		// 1. world: scent physics
		world.advanceScent();

		// 2. food: spawn + emit scent
		food.advance(current);

		// 3. predators hunt; strikes are applied by the ants context via events
		predators.advance(current);

		// 4. colony life; matured brood is spawned by the ants context via events
		colony.advance(current);

		// 5. all ants act
		ants.advance(current);

		if (broadcaster.hasSubscribers()) {
			broadcaster.broadcast(snapshots.current(current, colonyId, !paused, ticksPerSecond()));
		}

		if (current % 200 == 0) {
			log.info("Tick {}: {} ants ({}w/{}f), store {}, brood {}, food sources {}, birds {}",
					current, ants.aliveCount(),
					ants.countByRole(Role.WORKER), ants.countByRole(Role.FORAGER),
					colony.foodStore(colonyId).orElse(-1.0),
					colony.broodCount(colonyId).orElse(-1),
					food.sourceCount(), predators.birdCount());
		}
	}
}
