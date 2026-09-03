/**
 * Colony context — the social unit that lives in a nest.
 *
 * Owns the {@code Nest} aggregate: its entrance and chambers, the
 * {@code Queen}, the brood (eggs/larvae/pupae) that grows into new ants,
 * and the colony's stored food. Encodes the policies a real colony follows:
 * the queen lays eggs only while the colony is fed, brood matures into the
 * role the colony currently needs, and deposits of carried food grow the
 * store.
 *
 * Depends on {@code world} for positions (nest entrance coordinates).
 * The aggregates live in {@code colony.internal}; new adult ants are
 * announced via {@code AntHatched} events, which the {@code ants} context
 * listens to and turns into roaming adults.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "world")
@com.example.ddd.DDDBoundedContext(name = "colony", description = "The nest aggregate: queen, brood, stored food and the egg/hatch policies")
package com.example.antfarm.colony;
