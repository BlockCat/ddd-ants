/**
 * Simulation context — the engine that makes the world live.
 *
 * <p>Not a domain bounded context of the ant world itself but the application
 * layer around it: owns the simulation clock, tick cadence, the fixed
 * order in which contexts advance each tick, and mediates cross-context
 * effects (a bird attack becomes an ant death; a hatched brood member
 * becomes a new roaming ant). Publishes the significant domain events to
 * the outbox and exposes the world snapshot for rendering.
 *
 * <p>Depends on every domain module because it orchestrates them.
 *
 * <p><b>Public API</b> (kept deliberately small): {@code SimulationEngine}
 * (the application service), {@code SimulationSnapshot} (the view model
 * serialised to the browser) and {@code SimulationApiController} (the web
 * adapter owning the simulation endpoints). Everything else — the SSE
 * broadcaster, the outbox event logger, the snapshot builder, the typed
 * properties and the module configuration — is an implementation detail in
 * {@code simulation.internal} and off-limits to other modules.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "world", "colony", "ants", "food", "predators" })
@com.example.ddd.DDDBoundedContext(name = "simulation", description = "Tick engine and application layer that orchestrates the contexts")
package com.example.antfarm.simulation;
