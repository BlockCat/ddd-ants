package com.example.antfarm.food;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.antfarm.food.internal.FoodSource;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.WorldService;

/**
 * Public API of the food context.
 *
 * Owns the live food sources and the spawner policy: a new source appears
 * on free sand at a steady rate (up to a cap), every source emits food
 * scent into the world so foragers can smell it, and foragers draw sources
 * down until they are depleted and removed.
 */
@Service
@com.example.ddd.DDDApplicationService
public class FoodService {

	private static final Logger log = LoggerFactory.getLogger(FoodService.class);

	private final WorldService world;
	private final ApplicationEventPublisher events;
	private final Map<FoodId, FoodSource> sources = new LinkedHashMap<>();
	private final AtomicLong ids = new AtomicLong(1);

	private Random random = new Random(1);
	private int maxSources = 6;
	private int spawnIntervalTicks = 90;
	private double minAmount = 15;
	private double maxAmount = 45;
	private long lastSpawnTick = 0;

	public FoodService(WorldService world, ApplicationEventPublisher events) {
		this.world = world;
		this.events = events;
	}

	public void configure(int maxSources, int spawnIntervalTicks, double minAmount, double maxAmount, long randomSeed) {
		this.maxSources = maxSources;
		this.spawnIntervalTicks = spawnIntervalTicks;
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		this.random = new Random(randomSeed);
		log.info("Food spawner configured: cap {}, spawn every {} ticks, {}-{} per source",
				maxSources, spawnIntervalTicks, minAmount, maxAmount);
	}

	public int sourceCount() {
		return sources.size();
	}

	/** One tick of the food ecology: spawn, then emit scent from every source. */
	public void advance(long tick) {
		spawnIfDue(tick);
		for (FoodSource source : sources.values()) {
			world.emitFoodScent(source.position(), 1f);
		}
	}

	private void spawnIfDue(long tick) {
		if (sources.size() >= maxSources) {
			return;
		}
		if (tick - lastSpawnTick < spawnIntervalTicks) {
			return;
		}
		world.randomFreeSand(random).ifPresentOrElse(position -> {
			FoodId id = new FoodId(ids.getAndIncrement());
			double amount = minAmount + random.nextDouble() * (maxAmount - minAmount);
			FoodType type = FoodType.values()[random.nextInt(FoodType.values().length)];
			sources.put(id, new FoodSource(id, position, type, amount));
			lastSpawnTick = tick;
			log.info("Food source {} ({}) appeared at {} with {} food ({} active)",
					id, type, position, Math.round(amount), sources.size());
			events.publishEvent(new FoodSourceSpawned(id, position, type, amount, tick));
		}, () -> log.debug("Food spawn skipped: no free sand cell at tick {}", tick));
	}

	/** A source under an ant's feet, if any (returns id and remaining amount). */
	public Optional<FoodAt> foodAt(Position position) {
		for (FoodSource source : sources.values()) {
			if (source.position().equals(position) && !source.isEmpty()) {
				return Optional.of(new FoodAt(source.id(), source.amount()));
			}
		}
		return Optional.empty();
	}

	/**
	 * Forager takes up to {@code requested} food from a source. Returns the
	 * amount actually taken (0 when the source is gone); removes and announces
	 * the source when it is depleted.
	 */
	public double take(FoodId foodId, double requested, long tick) {
		FoodSource source = sources.get(foodId);
		if (source == null) {
			return 0;
		}
		double taken = source.takeUpTo(requested);
		if (source.isEmpty()) {
			sources.remove(foodId);
			log.info("Food source {} at {} depleted after {} total", foodId, source.position(), Math.round(source.amount() + taken));
			events.publishEvent(new FoodSourceDepleted(foodId, source.position(), tick));
		}
		return taken;
	}

	/** All live sources, for snapshots/rendering. */
	public List<FoodLocation> all() {
		return sources.values().stream()
				.map(s -> new FoodLocation(s.id().value(), s.type(), s.position(), s.amount()))
				.toList();
	}

	/** A source under an ant's feet. */
	public record FoodAt(FoodId foodId, double available) {
	}

	/** View of a live source for rendering. */
	public record FoodLocation(long id, FoodType type, Position position, double amount) {
	}
}
