package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <b>domain event</b>: a fact the business cares about, stated in
 * the past tense, that happened inside a context.
 *
 * <p>Events carry ids and values (never object references) so they can cross
 * module boundaries, be written to the outbox and be replayed. In this demo:
 * {@code EggLaid}, {@code AntHatched}, {@code FoodDeposited},
 * {@code FoodSourceDepleted}, {@code BirdAttacked}, {@code AntDied},
 * {@code ChamberDug}.
 *
 * <p>Descriptive only — delivery is provided by Spring Modulith's event
 * publication registry, not by this marker.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDEvent {
}
