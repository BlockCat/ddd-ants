# 🐜 DDD Demo — Ant Farm Simulation

A **Domain-Driven Design** teaching project: an ant farm simulation built as a
Spring Boot application whose bounded contexts are enforced as Spring
Modulith *application modules*.

The simulation models a colony of ants living in a sand world — workers that
dig nest chambers, foragers that find food by **pheromone scent** and carry it
home, a queen that lays eggs while the colony is fed — plus food sources that
spawn and deplete, and birds that prey on surface ants. The result is rendered
live in a browser with run controls.

> Status: **all milestones done.** World + colony (queen/eggs/brood) +
> roaming ants run on a deterministic tick engine; foragers follow a
> food-scent gradient and lay trails home (stigmergy); birds hunt surface
> ants; workers dig chambers; significant domain events flow through the
> transactional outbox into Postgres; a live HTML5 canvas viewer streams
> snapshots over SSE with pause/resume/speed controls and a legend.

## Why this project exists

It demonstrates, with one small codebase:

- **Strategic DDD**: ubiquitous language, bounded contexts, context map —
  the ant world split into `world`, `colony`, `ants`, `food`, `predators`,
  and the `simulation` engine.
- **Tactical DDD**: aggregates with invariants, value objects (`Position`,
  amounts), domain events, rich behaviour on the model.
- **DDD marker annotations**: `com.example.ddd` provides
  `@DDDAggregateRoot`, `@DDDEntity`, `@DDDValueObject`, `@DDDEvent`,
  `@DDDCommand`, `@DDDApplicationService`, `@DDDBoundedContext`, … —
  purely descriptive metadata that makes the building blocks visible in the
  code (nothing reads them at runtime).
- **Spring Modulith**: one application module per bounded context, public-API
  discipline between modules, structural verification, and a transactional
  **outbox** (event publication registry) for reliable cross-module events.
- **Deterministic simulation architecture**: a single-threaded tick engine,
  in-memory aggregate state for speed, and a durable, replayable trace of
  *significant* domain events (eggs laid, hatches, meals, deaths) in
  Postgres.

## Tech stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (Temurin LTS) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7) |
| Modularity | Spring Modulith 2.1.1 (BOM-managed) |
| Persistence | Spring Data JPA + Flyway + PostgreSQL |
| Events | Modulith event publication registry (outbox), `@ApplicationModuleListener` |
| API/rendering | REST + SSE snapshots, static HTML5 canvas client |
| Testing | JUnit 5, `@ApplicationModuleTest`/Scenario, Testcontainers |

## Project layout

```
ddd-demo/
├── pom.xml                          # Boot 4.1.1 parent, Modulith BOM 2.1.1
├── compose.yaml                     # local Postgres (podman/docker compose)
├── docs/                            # the DDD design contract (model first!)
│   ├── ubiquitous-language.md       # glossary: terms used verbatim in code
│   ├── context-map.md               # bounded contexts & relationships
│   ├── domain-events.md             # event catalog by owning context
│   └── simulation-design.md         # tick engine, persistence, rendering
└── src
    ├── main/java/com/example
    │   ├── ddd/         # DDD marker annotations (@DDDEvent, @DDDValueObject, …) —
    │   │                # purely descriptive, no behaviour, outside the module graph
    │   └── antfarm
    │       ├── AntFarmApplication.java
    │       ├── world/       # terrain grid, occupancy, scent fields
    │       ├── colony/      # events + service; internal/ = nest aggregates
    │       ├── ants/        # events + service; internal/ = ant aggregate
    │       ├── food/        # events + service; internal/ = food-source aggregate
    │       ├── predators/   # events + service; internal/ = bird aggregate
    │       └── simulation/  # tick engine, orchestration, snapshot API
    │           ├── SimulationEngine.java        # public application service
    │           ├── SimulationSnapshot.java      # public view model
    │           ├── SimulationApiController.java # public web adapter
    │           └── internal/                    # SSE, outbox listener, props, snapshot builder
    └── test/java/com/example/antfarm
        ├── ArchitectureTests.java  # Module boundary verification
        └── …                        # (DB-bound tests use Testcontainers)
```

## Prerequisites

- **JDK 21** (any distribution; installed via SDKMAN in this workspace:
  `sdk install java 21.0.12+1.1-tem`)
- **PostgreSQL 15+** — either
  - a container: [`compose.yaml`](compose.yaml) is provided — `podman compose
    up -d` (or `podman-compose up -d`, or `docker compose up -d`), or
  - a local server (this sandbox uses one installed via `apt`), or
  - the generated Testcontainers setup for tests on machines with a container
    runtime.
- **Maven** — use the included wrapper `./mvnw` (no global install needed).

## Quickstart

```bash
# environment (this workspace)
export JAVA_HOME="$HOME/.sdkman/candidates/java/current"
export PATH="$JAVA_HOME/bin:$HOME/.sdkman/candidates/maven/current/bin:$PATH"

# 1. start Postgres — podman compose up -d | docker compose up -d | local apt install
#    (db/user/password: antfarm, matching src/main/resources/application.properties)
#    No compose provider? Run the container directly:
#    podman run -d --name antfarm-postgres -p 127.0.0.1:5432:5432 \
#      -e POSTGRES_DB=antfarm -e POSTGRES_USER=antfarm -e POSTGRES_PASSWORD=antfarm \
#      -v postgres-data:/var/lib/postgresql/data docker.io/library/postgres:16-alpine
# 2. build
cd ddd-demo
./mvnw -q -DskipTests package

# 3. structural verification (no DB needed)
./mvnw test -Dtest=ArchitectureTests

# 4. run
./mvnw spring-boot:run        # then open http://localhost:8080

## Watch it live

Open <http://localhost:8080> — a static HTML5 canvas page renders the ant
farm in the browser:

- sand terrain with branches, pebbles and dug chambers; the nest entrance
  hole; food sources sized by remaining amount; flapping birds; ants
  coloured by role (workers brown, foragers orange), a green dot when
  carrying food
- **pheromone trails are visible**: laden foragers leave a fading trail on
  the way home, and foragers follow the scent gradient to food (the
  server-side field lives in the world module)
- snapshots pushed over **SSE** (`/api/sim/stream`, one `snapshot` event per
  tick) with an automatic polling fallback (`/api/sim/state`)
- a legend panel and run controls: pause/resume and speed (0.25×–8×)

API endpoints:

| Endpoint | Returns |
|---|---|
| `/api/sim/terrain` | grid `cells[y][x]` (terrain-kind ordinals) |
| `/api/sim/state` | current `SimulationSnapshot` |
| `/api/sim/stream` | live `text/event-stream` of snapshots |
| `/api/sim/status` | tick, paused, ticks/second |
| `POST /api/sim/pause` `resume` `speed?multiplier=` | run controls |

While it runs, watch the application log: domain events (egg laid, hatched,
deposited, food spawned/depleted, bird attacks, ant deaths) are written to
the `EVENT_PUBLICATION` outbox table in the same transaction as the tick and
delivered asynchronously after commit to the `ANTFARM.EVENTS` listener.

## Design in one page

Each top-level package under `com.example.antfarm` is one bounded context /
Modulith application module:

```
simulation ──► colony ──► world ◄── food
   │  └────► ants ──► colony ──► predators
   │            │             ▲
   └────────► food            │
   └────────► predators ──────┘
```

One-way effects between contexts are published as **events** and handled by
listeners in the owning context: `colony` publishes `AntHatched` and `ants`
spawns the adult; `predators` publishes `BirdAttacked` and `ants` kills the
victim. Request/response operations (feeding, picking food up) remain
synchronous commands.

Rules enforced automatically by `ArchitectureTests` (`ApplicationModules
.verify()`): no module may reach another module's internals, dependencies
must honour the declared `allowedDependencies`, no cycles.

**Read the docs — they are the model:**
1. [`docs/ubiquitous-language.md`](docs/ubiquitous-language.md) — the terms
2. [`docs/context-map.md`](docs/context-map.md) — the boundaries
3. [`docs/domain-events.md`](docs/domain-events.md) — the facts
4. [`docs/simulation-design.md`](docs/simulation-design.md) — how it runs

## Agent skills

The DDD, Spring Boot and Spring Modulith expertise used for this project
lives as loadable skills in `../.pi/skills/` (`ddd-expert`,
`spring-boot-expert`, `spring-modulith-expert`).

## Milestones

- **M0 — done** module skeleton + architecture verification + design docs
- **M1 — done** vertical slice: world grid, colony (queen, eggs, brood),
  roaming ants; deterministic tick engine; outbox-persisted events
- **M2 — done** foraging: food sources spawn/deplete, food-scent pheromone
  field, foragers follow the gradient and lay trails home, deposits feed
  the store economy
- **M3 — done** predators: birds patrol and hunt surface ants (event-driven
  kills via `BirdAttacked` → `AntDied(EATEN)`)
- **M4 — done** workers dig chambers out of the sand near the nest
- **M5 — done** live canvas viewer over SSE, legend, pause/resume/speed
