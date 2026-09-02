package com.example.antfarm.ants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.antfarm.ants.model.Ant;
import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyService;
import com.example.antfarm.colony.Role;
import com.example.antfarm.food.FoodService;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.WorldService;

/**
 * Public API of the ants context: owns the roaming adult ants and their
 * behaviour.
 *
 * Each tick every ant senses and acts. The behaviour set:
 *
 * <ul>
 *   <li><b>eat &amp; rest</b> inside the nest (drawing meals from the store),</li>
 *   <li><b>forage</b> — foragers follow the food-scent gradient (the
 *       pheromone field maintained by the world module) toward a source,
 *       pick food up, and lay scent while carrying it home,</li>
 *   <li><b>deposit</b> carried food into the colony store on arrival,</li>
 *   <li><b>dig</b> — workers occasionally carve chambers out of the sand
 *       near the nest, and</li>
 *   <li><b>die</b> when energy runs out (starving, or eaten by a bird via
 *       the engine).</li>
 * </ul>
 *
 * Movement goes through {@link WorldService}; meals and deposits through
 * {@link ColonyService}; food through {@link FoodService}. The ant
 * aggregate decides; the services own their state.
 */
@Service
@com.example.ddd.DDDApplicationService
public class AntService {

	private static final Logger log = LoggerFactory.getLogger(AntService.class);

	private final WorldService world;
	private final ColonyService colony;
	private final FoodService food;
	private final ApplicationEventPublisher events;
	private final Map<AntId, Ant> ants = new LinkedHashMap<>();
	private final AtomicLong ids = new AtomicLong(1);

	private AntPolicy policy = AntPolicy.DEFAULTS;
	private Random random = new Random(1);

	public AntService(WorldService world, ColonyService colony, FoodService food,
			ApplicationEventPublisher events) {
		this.world = world;
		this.colony = colony;
		this.food = food;
		this.events = events;
	}

	public void configure(AntPolicy policy, long randomSeed) {
		this.policy = policy;
		this.random = new Random(randomSeed);
		log.info("Ant behaviour configured: {}", policy);
	}

	/** Creates a new adult ant inside its nest. */
	public AntId spawn(SpawnAnt command) {
		Ant ant = new Ant(new AntId(ids.getAndIncrement()), command.colonyId(), command.role(),
				command.entrance(), policy.startEnergy());
		ants.put(ant.id(), ant);
		log.debug("Ant {} ({}) hatched into colony {} at nest {}", ant.id(), ant.role(), ant.colonyId(), ant.entrance());
		return ant.id();
	}

	/** Advances all ants one tick. */
	public void advance(long tick) {
		for (Ant ant : new ArrayList<>(ants.values())) {
			try {
				if (ant.isInside()) {
					advanceInside(ant, tick);
				} else {
					advanceOutside(ant, tick);
				}
			} catch (RuntimeException ex) {
				log.error("Tick {} failed for ant {}: {}", tick, ant.id(), ex.getMessage(), ex);
			}
		}
	}

	/** Engine-mediated death (bird attack, …). Returns false if the ant was already gone. */
	public boolean kill(AntId antId, AntDeathCause cause, long tick) {
		Ant ant = ants.get(antId);
		if (ant == null) {
			log.debug("Kill requested for unknown/dead ant {}", antId);
			return false;
		}
		die(ant, tick, cause, "was caught by a bird");
		return true;
	}

	// ------------------------------------------------------------------
	// Inside the nest: rest, feed, then leave again
	// ------------------------------------------------------------------

	private void advanceInside(Ant ant, long tick) {
		ant.spend(policy.insideCost());
		ant.tickInside();
		if (ant.energy() <= 0) {
			die(ant, tick, AntDeathCause.STARVED, "starved inside the nest");
			return;
		}
		if (ant.energy() < policy.eatThreshold()) {
			if (colony.tryConsumeFood(ant.colonyId(), policy.mealAmount())) {
				ant.refill(policy.mealAmount());
				log.debug("Ant {} fed {} at tick {} (energy {})", ant.id(), policy.mealAmount(), tick, round(ant.energy()));
			}
		}
		if (ant.energy() >= policy.leaveThreshold() && ant.insideTicks() >= policy.leaveIntervalTicks()) {
			leaveNest(ant, tick);
		}
	}

	private void leaveNest(Ant ant, long tick) {
		Optional<Position> spot = world.freeCellNear(ant.entrance(), 1, random);
		if (spot.isEmpty()) {
			log.debug("Ant {} cannot leave nest at {} — no free cell nearby (tick {})", ant.id(), ant.entrance(), tick);
			return;
		}
		if (!world.register(ant.id().value(), spot.get())) {
			log.warn("Ant {} lost the race for cell {} — staying inside", ant.id(), spot.get());
			return;
		}
		ant.setHeading(spot.get().x() - ant.entrance().x(), spot.get().y() - ant.entrance().y());
		double multiplier = ant.role() == Role.FORAGER ? 2.0 : 1.0;
		long budget = (long) (random.nextLong(policy.minExploreTicks(), policy.maxExploreTicks() + 1) * multiplier);
		ant.leaveNest(spot.get(), budget);
		log.debug("Ant {} ({}) left the nest to explore {} (budget {} ticks, energy {})",
				ant.id(), ant.role(), spot.get(), budget, round(ant.energy()));
	}

	// ------------------------------------------------------------------
	// Outside: forage (scent!), dig, explore, return home
	// ------------------------------------------------------------------

	private void advanceOutside(Ant ant, long tick) {
		ant.spend(policy.outsideCost());
		ant.markOutsideTick();
		if (ant.energy() <= 0) {
			die(ant, tick, AntDeathCause.STARVED, "starved outside");
			return;
		}
		if (ant.isReturning()) {
			if (ant.position().equals(ant.entrance())) {
				arriveHome(ant, tick);
			} else {
				if (ant.carrying() > 0) {
					// pheromone trick: a laden forager marks its way home (heavy trail)
					world.emitFoodScent(ant.position(), 1.0f);
				}
				stepTowards(ant, ant.entrance(), tick);
			}
			return;
		}
		advanceExploring(ant, tick);
	}

	private void advanceExploring(Ant ant, long tick) {
		// standing on food? pick it up and head home
		Optional<FoodService.FoodAt> underFoot = food.foodAt(ant.position());
		if (underFoot.isPresent()) {
			double taken = food.take(underFoot.get().foodId(), policy.carryingCapacity(), tick);
			if (taken > 0) {
				ant.addCarrying(taken);
				ant.startReturningHome();
				log.info("Ant {} ({}) collected {} food at {} and is heading home",
						ant.id(), ant.role(), round(taken), ant.position());
				return;
			}
		}
		// pheromone trick: foragers climb the food-scent gradient
		if (followFoodScent(ant, tick)) {
			return;
		}
		// trip over or hungry?
		if (ant.ticksOutside() >= ant.exploreBudget() || ant.energy() < policy.eatThreshold()) {
			ant.startReturningHome();
			log.debug("Ant {} turns home (budget spent or hungry, energy {})", ant.id(), round(ant.energy()));
			stepTowards(ant, ant.entrance(), tick);
			return;
		}
		// workers carve chambers near the nest
		if (ant.role() == Role.WORKER && random.nextDouble() < policy.digProbability()
				&& ant.position().distanceTo(ant.entrance()) <= 3) {
			if (tryDig(ant, tick)) {
				return;
			}
		}
		wander(ant, tick);
	}

	private boolean followFoodScent(Ant ant, long tick) {
		if (ant.role() != Role.FORAGER) {
			return false;
		}
		// Distance-scaled sensing: a candidate cell at distance d is "smelled"
		// as (scent - FALLOFF * d). Strong/heavy pheromones therefore register
		// from far away; faint traces only from up close — more pheromones =
		// larger sensing distance.
		float here = world.foodScentAt(ant.position());
		Position best = null;
		float bestScore = here;
		int r = policy.sniffRadius();
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				if (dx == 0 && dy == 0) {
					continue;
				}
				int nx = ant.position().x() + dx;
				int ny = ant.position().y() + dy;
				if (nx < 0 || ny < 0 || !world.isWalkable(new Position(nx, ny))) {
					continue;
				}
				float scent = world.foodScentAt(new Position(nx, ny));
				float score = scent - SCENT_FALLOFF * (Math.abs(dx) + Math.abs(dy));
				if (score > bestScore + 0.01f) {
					bestScore = score;
					best = new Position(nx, ny);
				}
			}
		}
		if (best == null) {
			return false;
		}
		stepTowards(ant, best, tick);
		return true;
	}

	private static final float SCENT_FALLOFF = 0.05f;

	private boolean tryDig(Ant ant, long tick) {
		List<Position> neighbours = world.freeNeighbours(ant.position());
		if (neighbours.isEmpty()) {
			return false;
		}
		Position target = neighbours.get(random.nextInt(neighbours.size()));
		if (!world.dig(target)) {
			return false;
		}
		ant.spend(policy.digCost());
		events.publishEvent(new ChamberDug(ant.id(), ant.colonyId(), target, tick));
		log.debug("Worker {} dug a chamber at {}", ant.id(), target);
		return true;
	}

	private void wander(Ant ant, long tick) {
		List<Position> options = world.freeNeighbours(ant.position());
		if (options.isEmpty()) {
			log.debug("Ant {} is boxed in at {}", ant.id(), ant.position());
			return;
		}
		// momentum: keep moving in the current direction rather than turning at
		// random on every tick
		move(ant, chooseStep(ant, options, null), tick);
	}

	private void stepTowards(Ant ant, Position goal, long tick) {
		List<Position> options = world.freeNeighbours(ant.position());
		if (options.isEmpty()) {
			log.debug("Ant {} is blocked at {} (goal {})", ant.id(), ant.position(), goal);
			return;
		}
		// greedy, momentum-biased: prefer the option(s) closest to the goal, and
		// among ties the step that keeps the current heading
		move(ant, chooseStep(ant, options, goal), tick);
	}

	/**
	 * Picks a step among the options. When a {@code goal} exists only the
	 * closest-to-goal options compete; the winner is chosen by heading
	 * momentum (straight ahead ≫ sideways ≫ U-turn), so movement is smooth
	 * instead of a fresh random decision every tick.
	 */
	private Position chooseStep(Ant ant, List<Position> options, Position goal) {
		List<Position> candidates = options;
		if (goal != null) {
			int bestDistance = Integer.MAX_VALUE;
			for (Position option : options) {
				bestDistance = Math.min(bestDistance, option.distanceTo(goal));
			}
			candidates = new ArrayList<>();
			for (Position option : options) {
				if (option.distanceTo(goal) == bestDistance) {
					candidates.add(option);
				}
			}
		}
		double total = 0;
		double[] weights = new double[candidates.size()];
		for (int i = 0; i < candidates.size(); i++) {
			weights[i] = headingWeight(ant, candidates.get(i));
			total += weights[i];
		}
		double pick = random.nextDouble() * total;
		for (int i = 0; i < weights.length; i++) {
			pick -= weights[i];
			if (pick <= 0) {
				return candidates.get(i);
			}
		}
		return candidates.get(candidates.size() - 1);
	}

	/** Momentum weight of a candidate step relative to the ant's heading. */
	private double headingWeight(Ant ant, Position candidate) {
		int dx = Integer.compare(candidate.x(), ant.position().x());
		int dy = Integer.compare(candidate.y(), ant.position().y());
		int dot = dx * ant.headingX() + dy * ant.headingY();
		return switch (dot) {
			case 1 -> 5.0;  // straight ahead — keep going
			case 0 -> 2.0;  // turn sideways
			default -> 0.5; // U-turn (usually only when boxed in)
		};
	}

	private void move(Ant ant, Position to, long tick) {
		if (world.move(ant.id().value(), ant.position(), to)) {
			int fromX = ant.position().x();
			int fromY = ant.position().y();
			ant.setPosition(to);
			ant.setHeading(to.x() - fromX, to.y() - fromY);
		} else {
			log.warn("Ant {} could not move {} -> {} at tick {}", ant.id(), ant.position(), to, tick);
		}
	}

	private void arriveHome(Ant ant, long tick) {
		double carried = ant.takeCarrying();
		if (carried > 0) {
			colony.depositFood(ant.colonyId(), ant.id().value(), carried, tick);
			log.info("Ant {} deposited {} food into colony {} store", ant.id(), round(carried), ant.colonyId());
		}
		world.unregister(ant.id().value(), ant.position());
		ant.enterNest();
		log.debug("Ant {} returned to the nest at tick {} (energy {})", ant.id(), tick, round(ant.energy()));
	}

	// ------------------------------------------------------------------

	private void die(Ant ant, long tick, AntDeathCause cause, String reason) {
		if (!ant.isInside()) {
			world.unregister(ant.id().value(), ant.position());
		}
		ants.remove(ant.id());
		events.publishEvent(new AntDied(ant.id(), ant.colonyId(), cause, tick));
		switch (cause) {
			case STARVED -> log.warn("Ant {} {} ({}, colony {})", ant.id(), reason, cause, ant.colonyId());
			default -> log.info("Ant {} {} ({}, colony {})", ant.id(), reason, cause, ant.colonyId());
		}
	}

	private static double round(double value) {
		return Math.round(value * 10) / 10.0;
	}

	public int aliveCount() {
		return ants.size();
	}

	public long countByRole(Role role) {
		return ants.values().stream().filter(a -> a.role() == role).count();
	}

	/** View of one roaming (outside) ant, for rendering and snapshots. */
	public record RoamingAnt(AntId id, Role role, Position position, double energy, double carrying) {
	}

	/** All ants currently outside their nest, with positions. */
	public List<RoamingAnt> roaming() {
		return ants.values().stream()
				.filter(a -> !a.isInside())
				.map(a -> new RoamingAnt(a.id(), a.role(), a.position(), a.energy(), a.carrying()))
				.toList();
	}

	public List<AntId> aliveAnts() {
		return ants.values().stream().map(Ant::id).toList();
	}

	/** Position of a roaming ant, if it is currently outside. */
	public Optional<Position> positionOf(AntId id) {
		Ant ant = ants.get(id);
		return ant == null || ant.isInside() ? Optional.empty() : Optional.of(ant.position());
	}
}
