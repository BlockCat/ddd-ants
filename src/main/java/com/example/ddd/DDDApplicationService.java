package com.example.ddd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an <b>application service</b>: a thin orchestrator between the
 * outside world and the model. It takes commands/DTOs, loads aggregates via
 * repositories, calls domain behaviour, publishes events and manages the
 * transaction. It contains <b>no business rules</b> — only coordination.
 *
 * <p>In this demo the module entry points ({@code ColonyService},
 * {@code AntService}, {@code FoodService}, {@code PredatorService},
 * {@code WorldService}, {@code SimulationEngine}) are the application layer.
 *
 * <p>Descriptive only.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DDDApplicationService {
}
