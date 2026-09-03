# Simulation Design

How the whole thing runs: engine, tick pipeline, event delivery, persistence,
rendering.

## Runtime architecture

```
Browser (HTML5 canvas, static JS)
   ▲  SSE: /api/sim/stream   (snapshots, ~10 Hz)
   │  REST: /api/sim/state, /api/sim/terrain, /api/sim/status
┌──┴─────────────────────────────────────────────────────────────┐
│ Spring Boot app (single JVM)                                    │
│                                                                 │
│  simulation module (application layer)                          │
│    TickScheduler (@Scheduled single thread, e.g. 10 Hz)         │
│    SimulationEngine.runTick(): fixed pipeline per tick          │
│    publishes events in one @Transactional block → outbox        │
│                                                                 │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │
│  │ world  │ │ colony │ │  ants  │ │  food  │ │predators│       │
│  │(World  │ │(state) │ │(state) │ │(state) │ │ (state) │        │
│  │ iface) │ │        │ │        │ │        │ │         │        │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘        │
│  in-memory aggregates; domain contexts talk only via events     │
│                                                                 │
│  outbox (EVENT_PUBLICATION, Postgres) → @ApplicationModuleListener│
│   → event log / read models (async, after commit)               │
└─────────────────────────────────────────────────────────────────┘
```

## Communication model

- **Domain contexts (`colony`, `ants`, `food`, `predators`) never call each
  other.** Requests, responses and facts are all events
  (`FoodConsumptionRequested → FoodGranted`, `FoodDelivered →
  FoodDeposited`, `FoodPickupRequested → FoodPicked`, `AntHatched`,
  `BirdAttacked` …).
- **State-changing reactions are synchronous** (`@EventListener`, same
  thread) so an effect lands in the same tick and the run stays
  deterministic.
- **Every event is also consumed asynchronously** by `@ApplicationModuleListener`
  consumers in the simulation module. Spring Modulith registers each
  publication in the `EVENT_PUBLICATION` outbox inside the tick transaction
  and delivers it after commit — the event trace is durable and replayable.
- **The world is the only synchronous dependency.** It is a spatial
  substrate accessed only through its `World` interface (movement,
  occupancy, scent, digging). It publishes no business events.
- **The simulation module is the application layer**: it drives the tick,
  reads module public APIs for snapshots, and translates foreign facts into
  owning-context commands (a `BirdAttacked` becomes `ants.kill(...)`, so
  ants never depends on predators).

## Tick pipeline (fixed order, one thread — deterministic)

Per tick `t`:

1. `world.advanceScent()` — evaporate/diffuse pheromones.
2. `food.advance()` — maybe spawn a source (if below cap); emit food scent.
   Spawning publishes `FoodSourceSpawned`, which the ants context projects
   into its local read model.
3. `predators.advance()` — move each bird; a strike publishes
   `BirdAttacked`. The simulation engine listens synchronously and calls
   `ants.kill(...)`; ants publishes `AntDied(cause=EATEN)`.
4. `colony.advance()` — queen may lay (`EggLaid`), brood matures
   (`AntHatched`); ants listens and spawns the adult.
5. `ants.advance()` — each ant acts: feed (meal request-event), leave the
   nest, forage (scent gradient), pick food (pickup request-event), carry
   home, deliver (delivery request-event), dig, starve/die.
6. `simulation` commits the tick transaction → outbox rows are durable →
   async `@ApplicationModuleListener`s run and append the event log; the SSE
   broadcaster pushes the snapshot.

Rationale: behaviours read state (positions, scents) at decision time and
write through the owning module's API or events; no two threads ever touch
live state; request/response across contexts is expressed as events so no
domain module reaches into another.

## Why in-memory state + outbox events (not full JPA state)

- A realtime simulation mutates hundreds of entities ~10×/second; persisting
  full aggregate state per tick would dominate the demo and teach the wrong
  lesson. Aggregates are still real (rich, invariant-guarding, encapsulated)
  but live in memory for the run.
- **Persistence shows up where it matters**: the significant-event trace is
  durable via Spring Modulith's JPA event-publication registry (the
  transactional outbox pattern), and async listeners build read models in
  Postgres. The event log makes the DDD story visible: open a table and
  *read the colony's story* (eggs laid, hatches, meals, deliveries, deaths).

## Persistence pieces

- Flyway migration `V1__init.sql`: creates `EVENT_PUBLICATION` (managed by
  Modulith) plus read-model tables (`colony_event_log`, `food_source_log` …).
- Repositories for read models only (domain aggregates are in-memory).
- Requires PostgreSQL:
  - This sandbox (no container runtime): PostgreSQL installed via `apt`, dev
    profile points at `localhost:5432`.
  - Other machines: [`compose.yaml`](../compose.yaml) (Podman or Docker
    compose) with a `postgres` service; tests use the generated
    Testcontainers setup.

## Rendering

- Backend: SSE endpoint pushing a compact snapshot JSON per tick (entities by
  type with positions + terrain). Snapshot built by the `simulation` module
  from module public APIs (it already depends on all).
- Frontend: static `index.html` + `canvas` renderer served from
  `resources/static` — sand background, branch/pebble cells, nest entrance,
  ants as moving dots coloured by role, food sources, birds; optional scent
  heatmap toggle. No JS build step.
- Polling fallback endpoint so the client works even without SSE.

## Determinism & scale

- Single-threaded scheduler; `System.nanoTime` only used for pacing, all
  sim decisions use the tick counter (reproducible).
- Starting config: world 120×80, 1 colony (~1 queen + ~14 workers +
  ~8 foragers), food cap ~14 sources, 2 birds, 10 ticks/s. All knobs via
  `@ConfigurationProperties` (`simulation.*`, e.g. `simulation.world.width`).

## Milestones (all implemented)

1. module skeleton + architecture verification + docs
2. vertical slice: world + colony (queen, eggs, brood) + roaming ants
3. foraging: food sources, food-scent pheromones, trail-laying, deliveries
4. predators: birds hunt → application-mediated ant deaths
5. workers: digging chambers into the sand
6. live HTML5 canvas viewer (SSE + fallback), legend, pause/resume/speed
7. event-only domain communication + `World` interface + outbox persistence
