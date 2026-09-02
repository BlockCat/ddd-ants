/**
 * Simulation context — the engine that makes the world live.
 *
 * Not a domain bounded context of the ant world itself but the application
 * layer around it: owns the simulation clock, tick cadence, the fixed
 * order in which contexts advance each tick, and mediates cross-context
 * effects (a bird attack becomes an ant death; a hatched brood member
 * becomes a new roaming ant). Publishes the significant domain events to
 * the outbox and exposes the world snapshot for rendering.
 *
 * Depends on every domain module because it orchestrates them.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "world", "colony", "ants", "food", "predators" })
@com.example.ddd.DDDBoundedContext(name = "simulation", description = "Tick engine and application layer that orchestrates the contexts")
package com.example.antfarm.simulation;
