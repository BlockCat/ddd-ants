package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an <b>aggregate root</b>: the entry point of an aggregate — a
 * cluster of entities and value objects treated as one consistency unit.
 *
 * <p>All external access goes through the root; invariants spanning the
 * cluster are enforced here; the root is the transactional consistency
 * boundary. In this demo: {@code Colony}, {@code Ant}, {@code FoodSource}.
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDAggregateRoot {
}
