# Simulation Design

How the whole thing runs: engine, tick pipeline, persistence, rendering.

## Runtime architecture

```
Browser (HTML5 canvas, static JS)
   ▲  SSE: /api/simulation/stream   (snapshots, ~8-10 Hz)
   │  REST: /api/simulation  (full state on demand), /api/simulation/history
┌──┴─────────────────────────────────────────────────────────────┐
│ Spring Boot app (single JVM)                                    │
│                                                                 │
│  simulation module (engine)                                     │
│    TickScheduler (@Scheduled single thread, e.g. 10 Hz)         │
│    SimulationService.advance(): fixed pipeline per tick         │
│    publishes events in one @Transactional block → outbox        │
│                                                                 │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │
│  │ world  │ │ colony │ │  ants  │ │  food  │ │predators│       │
│  │(state) │ │(state) │ │(state) │ │(state) │ │ (state) │        │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘        │
│  in-memory aggregates; module public APIs only                  │
│                                                                 │
│  outbox (EVENT_PUBLICATION, Postgres) → async listeners         │
│   → EventLog/history read models (JPA repositories)             │
└─────────────────────────────────────────────────────────────────┘
```

## Tick pipeline (fixed order, one thread — deterministic)

Per tick `t`:

1. `world.advance()` — evaporate/diffuse scent fields.
2. `food.advance()` — maybe spawn a source (if below cap); emit food scent
   from live sources; expire none (depletion only by eating).
3. `predators.advance()` — move each bird; choose a hunt; return attacks
   `(birdId, antId, position)`.
4. **Mediation 1** — engine calls `ants.kill(attack)` for each: ant dies,
   `ants` emits `AntDied(cause=EATEN)`.
5. `ants.advance()` — each live ant senses and acts (dig / forage / carry /
   return / rest / feed), consuming energy; ants at 0 energy in nest feed
   from store; if store empty → `AntStarved`.
6. `colony.advance()` — queen lays eggs if store above threshold; brood
   matures; returns hatch list `(role, entrance)`.
7. **Mediation 2** — engine calls `ants.spawn(hatch)` for each → new adult
   ant placed at nest entrance.
8. `simulation` publishes the tick's significant events inside one
   `@Transactional`, updates tick counter; SSE broadcaster pushes snapshot.

Rationale: behaviours read state (positions, scents) at decision time and
write through the owning module's API; no two threads ever touch live state.

## Why in-memory state + outbox events (not full JPA state)

- A realtime simulation mutates hundreds of entities ~10×/second; persisting
  full aggregate state per tick would dominate the demo and teach the wrong
  lesson. Aggregates are still real (rich, invariant-guarding, encapsulated)
  but live in memory for the run.
- **Persistence shows up where it matters**: the significant-event trace is
  durable via Spring Modulith's JPA event-publication registry (the
  transactional outbox pattern from the skill), and async listeners build
  read models (colony history, food-source log) in Postgres with JPA
  repositories + Flyway migrations.
- The event log makes the DDD story visible: open a table and *read the
  colony's story* (eggs laid, hatches, meals, deaths).

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
  type with positions + terrain change deltas). Snapshot built by the
  `simulation` module from module public APIs (it already depends on all).
- Frontend: static `index.html` + `canvas` renderer served from
  `resources/static` — sand background, branch/pebble cells, nest entrance,
  ants as moving dots coloured by role, food sources, birds; optional scent
  heatmap toggle. No JS build step.
- Polling fallback endpoint so the client works even without SSE.

## Determinism & scale

- Single-threaded scheduler; `System.nanoTime` only used for pacing, all
  sim decisions use the tick counter (reproducible).
- Starting config: world 120×80, 1 colony (~1 queen + ~25 workers +
  ~15 foragers), food cap ~6 sources, 1–2 birds, 10 ticks/s. All knobs via
  `@ConfigurationProperties` (`simulation.*`, e.g.
  `simulation.world.width`).

## Milestones (all implemented)

1. module skeleton + architecture verification + docs
2. vertical slice: world + colony (queen, eggs, brood) + roaming ants
3. foraging: food sources, food-scent pheromones, trail-laying, deposits
4. predators: birds hunt → engine-mediated ant deaths
5. workers: digging chambers into the sand
6. live HTML5 canvas viewer (SSE + fallback), legend, pause/resume/speed
