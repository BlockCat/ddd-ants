package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an <b>entity</b>: an object with identity and a lifecycle whose
 * state changes over time (equality = identity, never attributes).
 *
 * <p>Entities that are part of an aggregate but not its root (here:
 * {@code Queen} in the colony aggregate, {@code Bird}) are annotated with
 * this marker; aggregates of one that guard their own invariants are marked
 * {@link DDDAggregateRoot}.
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDEntity {
}
