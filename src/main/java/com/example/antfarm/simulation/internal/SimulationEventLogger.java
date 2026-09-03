package com.example.antfarm.simulation.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.springframework.modulith.events.ApplicationModuleListener;

import com.example.antfarm.ants.AntDied;
import com.example.antfarm.ants.ChamberDug;
import com.example.antfarm.colony.AntHatched;
import com.example.antfarm.colony.EggLaid;
import com.example.antfarm.colony.FoodDeposited;
import com.example.antfarm.food.FoodSourceDepleted;
import com.example.antfarm.food.FoodSourceSpawned;
import com.example.antfarm.predators.BirdAttacked;

/**
 * Cross-module consumer of the significant domain events, demonstrating the
 * Modulith outbox loop:
 *
 * <ol>
 *   <li>the producing context publishes inside the tick transaction,</li>
 *   <li>the event publication registry (EVENT_PUBLICATION table) records a
 *       pending delivery in the same transaction,</li>
 *   <li>this listener runs asynchronously <em>after commit</em>
 *       (@ApplicationModuleListener), and</li>
 *   <li>the publication is marked completed on success.</li>
 * </ol>
 *
 * It only observes — live simulation state is never touched from here, so
 * the async thread cannot race the tick thread.
 */
@Component
public class SimulationEventLogger {

	private static final Logger log = LoggerFactory.getLogger("ANTFARM.EVENTS");

	@ApplicationModuleListener
	void onEggLaid(EggLaid event) {
		log.info("event  egg-laid         {} tick={} (async, after commit)", event.colonyId(), event.tick());
	}

	@ApplicationModuleListener
	void onAntHatched(AntHatched event) {
		log.info("event  ant-hatched      {} role={} tick={}", event.colonyId(), event.role(), event.tick());
	}

	@ApplicationModuleListener
	void onFoodDeposited(FoodDeposited event) {
		log.info("event  food-deposited   {} ant={} amount={} store={} tick={}",
				event.colonyId(), event.antId(), Math.round(event.amount()),
				Math.round(event.storeAfter()), event.tick());
	}

	@ApplicationModuleListener
	void onAntDied(AntDied event) {
		log.info("event  ant-died         {} cause={} tick={}", event.antId(), event.cause(), event.tick());
	}

	@ApplicationModuleListener
	void onFoodSourceSpawned(FoodSourceSpawned event) {
		log.info("event  food-spawned     {} {} at {} amount={} tick={}",
				event.foodId(), event.type(), event.position(), Math.round(event.amount()), event.tick());
	}

	@ApplicationModuleListener
	void onFoodSourceDepleted(FoodSourceDepleted event) {
		log.info("event  food-depleted    {} at {} tick={}", event.foodId(), event.position(), event.tick());
	}

	@ApplicationModuleListener
	void onBirdAttacked(BirdAttacked event) {
		log.warn("event  bird-attacked    {} hit ant {} at {} tick={}",
				event.birdId(), event.antId(), event.position(), event.tick());
	}

	@ApplicationModuleListener
	void onChamberDug(ChamberDug event) {
		log.debug("event  chamber-dug      {} by {} tick={}", event.position(), event.antId(), event.tick());
	}
}
