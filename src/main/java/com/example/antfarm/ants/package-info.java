/**
 * Ants context — the free-roaming adult ants of a colony.
 *
 * Owns the {@code Ant} aggregate: identity, colony membership, role
 * (worker, forager, …), position, energy and current task. Ant behaviour is
 * the heart of the simulation: sense the terrain (scent gradients, food,
 * home), decide, move — dig, forage, carry food home, rest.
 *
 * Depends on {@code world} (positions, movement, scents), {@code food}
 * (picking up from a source, and a local read model of food positions),
 * and {@code colony} (meals and deliveries, announced as events).
 * Cross-module effects aimed at ants arrive as events: {@code AntHatched}
 * from {@code colony} spawns a new adult, and the engine translates a
 * predators strike into this context's {@code kill} command — ants never
 * reaches into (and never depends on) the {@code predators} module.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "world", "food", "colony" })
@com.example.ddd.DDDBoundedContext(name = "ants", description = "Free-roaming adults: forage by scent, carry food home, dig chambers, starve")
package com.example.antfarm.ants;
