/**
 * Predators context — the birds that prey on surface ants.
 *
 * Owns the {@code Bird} entity: its patrol route, hunger and hunting
 * behaviour. Birds catch ants found on open terrain and announce the fact;
 * the effect on the ant is handled in the {@code ants} context (via the
 * simulation engine), so this module never mutates another module's
 * aggregates.
 *
 * Depends on {@code world} for movement and for spotting ants on the
 * occupancy grid.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "world")
@com.example.ddd.DDDBoundedContext(name = "predators", description = "Birds that patrol and strike at ants on open sand")
package com.example.antfarm.predators;
