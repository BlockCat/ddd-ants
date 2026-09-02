package com.example.antfarm.predators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.antfarm.predators.model.Bird;
import com.example.antfarm.world.Position;
import com.example.antfarm.world.WorldService;

/**
 * Public API of the predators context.
 *
 * Owns the birds that patrol the sky over the world. A bird flies anywhere
 * (it is not bound by terrain), drifts around, and every so often strikes at
 * an ant spotted on open sand below. Attacks are returned to the engine,
 * which applies them in the ants context.
 */
@Service
@com.example.ddd.DDDApplicationService
public class PredatorService {

	private static final Logger log = LoggerFactory.getLogger(PredatorService.class);

	private final WorldService world;
	private final ApplicationEventPublisher events;
	private final Map<BirdId, Bird> birds = new LinkedHashMap<>();
	private final AtomicLong ids = new AtomicLong(1);

	private Random random = new Random(1);
	private int huntIntervalTicks = 120;
	private int huntRadius = 8;
	private int moveEveryTicks = 2;

	public PredatorService(WorldService world, ApplicationEventPublisher events) {
		this.world = world;
		this.events = events;
	}

	public void configure(int birdCount, int huntIntervalTicks, int huntRadius, int moveEveryTicks, long randomSeed) {
		this.huntIntervalTicks = huntIntervalTicks;
		this.huntRadius = huntRadius;
		this.moveEveryTicks = moveEveryTicks;
		this.random = new Random(randomSeed);
		for (int i = 0; i < birdCount; i++) {
			spawnBird();
		}
		log.info("Predators configured: {} birds, hunt every {} ticks, radius {}", birdCount, huntIntervalTicks, huntRadius);
	}

	private void spawnBird() {
		Position start = new Position(random.nextInt(world.width()), random.nextInt(world.height()));
		BirdId id = new BirdId(ids.getAndIncrement());
		birds.put(id, new Bird(id, start, random.nextInt(huntIntervalTicks)));
		log.debug("Bird {} took to the sky at {}", id, start);
	}

	public int birdCount() {
		return birds.size();
	}

	/**
	 * Advances all birds one tick: drift, then possibly hunt. Returns the
	 * attacks decided this tick for the engine to apply.
	 */
	public List<BirdAttack> advance(long tick) {
		List<BirdAttack> attacks = new ArrayList<>();
		for (Bird bird : birds.values()) {
			if (tick % moveEveryTicks == 0) {
				drift(bird);
			}
			if (bird.readyToHunt(tick)) {
				strike(bird, tick).ifPresent(attacks::add);
			}
		}
		return attacks;
	}

	private void drift(Bird bird) {
		int dx = random.nextInt(3) - 1; // -1..1
		int dy = random.nextInt(3) - 1;
		int x = Math.max(0, Math.min(world.width() - 1, bird.position().x() + dx));
		int y = Math.max(0, Math.min(world.height() - 1, bird.position().y() + dy));
		bird.moveTo(new Position(x, y));
	}

	private java.util.Optional<BirdAttack> strike(Bird bird, long tick) {
		List<Long> victims = world.occupantIdsNear(bird.position(), huntRadius);
		if (victims.isEmpty()) {
			bird.armHunt(tick, huntIntervalTicks);
			return java.util.Optional.empty();
		}
		long victim = victims.get(random.nextInt(victims.size()));
		bird.armHunt(tick, huntIntervalTicks);
		log.info("Bird {} swoops on ant {} at {} (tick {})", bird.id(), victim, bird.position(), tick);
		events.publishEvent(new BirdAttacked(bird.id(), victim, bird.position(), tick));
		return java.util.Optional.of(new BirdAttack(bird.id(), victim));
	}

	/** All birds, for snapshots/rendering. */
	public List<BirdLocation> all() {
		return birds.values().stream()
				.map(b -> new BirdLocation(b.id().value(), b.position()))
				.toList();
	}

	/** View of a bird for rendering. */
	public record BirdLocation(long id, Position position) {
	}
}
