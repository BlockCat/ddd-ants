/**
 * Ants context — the free-roaming adult ants of a colony.
 *
 * Owns the {@code Ant} aggregate: identity, colony membership, role
 * (worker, forager, …), position, energy and current task. Ant behaviour is
 * the heart of the simulation: sense the terrain (scent gradients, food,
 * home), decide, move — dig, forage, carry food home, rest.
 *
 * Depends on {@code world} (positions, movement, scents), {@code food}
 * (picking up from a source), and {@code colony} (depositing carried food,
 * consuming stored food, learning the nest entrance). Cross-module effects
 * aimed at ants arrive as events: {@code AntHatched} from {@code colony}
 * spawns a new adult, {@code BirdAttacked} from {@code predators} applies a
 * death — no other module reaches into this one.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "world", "food", "colony", "predators" })
@com.example.ddd.DDDBoundedContext(name = "ants", description = "Free-roaming adults: forage by scent, carry food home, dig chambers, starve")
package com.example.antfarm.ants;
