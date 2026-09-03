package com.example.antfarm.colony;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.antfarm.colony.internal.Colony;
import com.example.antfarm.world.Position;

/**
 * Public API of the colony context (in-memory nests for this increment).
 *
 * Owns colony aggregates and the colony policies: egg laying when fed,
 * brood maturation into castes, and the food store that hungry ants draw
 * from. New adults are announced via the {@link AntHatched} event; the ants
 * context listens to it and creates the roaming ant — no direct
 * cross-module call needed.
 */
@Service
@com.example.ddd.DDDApplicationService
public class ColonyService {

	private static final Logger log = LoggerFactory.getLogger(ColonyService.class);

	private final ApplicationEventPublisher events;
	private final Map<ColonyId, Colony> colonies = new LinkedHashMap<>();
	private final AtomicLong ids = new AtomicLong(1);

	public ColonyService(ApplicationEventPublisher events) {
		this.events = events;
	}

	public ColonyId createColony(Position entrance, double initialFood, ColonyPolicy policy) {
		ColonyId id = new ColonyId(ids.getAndIncrement());
		colonies.put(id, new Colony(id, entrance, initialFood, policy));
		log.info("Colony {} established at {} with {} food (policy: {})", id, entrance, initialFood, policy);
		return id;
	}

	public Optional<Double> foodStore(ColonyId id) {
		return colony(id).map(Colony::food);
	}

	public Optional<Position> entranceOf(ColonyId id) {
		return colony(id).map(Colony::entrance);
	}

	public Optional<Integer> broodCount(ColonyId id) {
		return colony(id).map(Colony::broodCount);
	}

	/**
	 * Feeds an ant from the store. Returns whether food was granted.
	 * Logs a warning when the store runs dry (ants will starve afterwards).
	 */
	public boolean tryConsumeFood(ColonyId id, double amount) {
		Optional<Colony> maybe = colony(id);
		if (maybe.isEmpty()) {
			log.warn("Cannot feed unknown colony {}", id);
			return false;
		}
		Colony colony = maybe.get();
		double before = colony.food();
		boolean granted = colony.tryConsume(amount);
		if (granted) {
			log.debug("Colony {} fed {} food (store {} -> {})", id, amount, before, colony.food());
		} else if (before > 0) {
			log.warn("Colony {} store {} cannot cover a meal of {} — starvation imminent", id, before, amount);
		}
		return granted;
	}

	public void depositFood(ColonyId id, long antId, double amount, long tick) {
		colony(id).ifPresentOrElse(c -> {
			c.deposit(amount);
			log.debug("Colony {} store now {} after deposit of {} by ant {}", id, c.food(), amount, antId);
			events.publishEvent(new FoodDeposited(id, antId, amount, c.food(), tick));
		}, () -> log.warn("Cannot deposit into unknown colony {}", id));
	}

	/**
	 * Advances all colonies one tick. Matured brood is announced via the
	 * {@link AntHatched} event; the ants context reacts by spawning the adult.
	 */
	public void advance(long tick) {
		for (Colony colony : colonies.values()) {
			Colony.ColonyTickResult result = colony.advance(tick);
			if (result.eggLaid()) {
				EggLaid eggLaid = new EggLaid(colony.id(), tick);
				events.publishEvent(eggLaid);
				log.debug("Published {} (brood now {})", eggLaid, colony.broodCount());
			}
			if (!result.hatched().isEmpty()) {
				log.debug("Colony {} matured {} brood at tick {}", colony.id(), result.hatched().size(), tick);
			}
			for (Role role : result.hatched()) {
				AntHatched event = new AntHatched(colony.id(), role, colony.entrance(), tick);
				events.publishEvent(event);
				log.debug("Published {}", event);
			}
		}
	}

	private Optional<Colony> colony(ColonyId id) {
		return Optional.ofNullable(colonies.get(id));
	}
}
