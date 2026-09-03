# Context Map — Ant Farm Simulation

Six bounded contexts, each a Spring Modulith application module. The core
domain is **ants** (the behaviour rules that make the simulation live); the
world is the spatial substrate everything depends on.

## Contexts

| Context (module) | Type | Responsibility | Language highlights |
|---|---|---|---|
| `world` | Supporting (spatial substrate) | Grid of terrain cells (sand/branch/pebble/chamber), occupancy of entities, pheromone/scent fields, passability & sensing. Leaf module. | Cell, Sand, Branch, Position |
| `colony` | Core | Nest aggregate: queen, brood, stored food, entrance & chambers; policies: lay eggs when fed, mature brood into needed role. | Nest, Queen, Brood, FoodStore |
| `ants` | Core | Free-roaming adults: role behaviour, energy, tasks (forage, dig, rest, return, deposit), life & death. | Ant, Worker, Forager |
| `food` | Supporting | Food sources: amount, depletion, respawn policy; food scent emission. | FoodSource |
| `predators` | Supporting | Birds: patrol, hunger, hunting surface ants. | Bird |
| `simulation` | Generic/application (not domain) | Clock, tick cadence, tick order, cross-context mediation, event publication to outbox, snapshot for rendering. | Tick |

## Relationships (module dependencies = declared allowedDependencies)

```
simulation ──► colony ──► world ◄── food
   │  └────► ants ──► colony           │
   │            ▲                      │
   └────────► food                     │
   └────────► predators ─────────────►┘
```

| Upstream | Downstream | Relationship | Mechanism | Translation owner |
|---|---|---|---|---|
| `world` | everyone | Customer–supplier: world supplies space & sensing; consumers adapt to its grid API | Synchronous public API calls (position, passability, scent read/write, occupancy) | Consumers |
| `food` | `ants` | Customer–supplier: foragers draw food down at a source | Synchronous command in tick (forager → `food.take(sourceId, amount)`) | `ants` |
| `colony` | `ants` | Customer–supplier: ants deposit carried food and feed from the store | Synchronous command API of `colony` (request/response) | `ants` |
| `colony` | `ants` | New adults announced | Domain event `AntHatched` (colony owns it) | `colony` publishes, `ants` listens and spawns |
| `predators` | `ants` | Bird attacks become ant deaths | Domain event `BirdAttacked` (predators owns it) | `predators` publishes, `ants` listens and kills → publishes `AntDied` |
| all contexts | `simulation` | Significant facts recorded / observable | Domain events → outbox (event publication registry) → async listeners append history | publisher of each event |

## Notes on the seams

- **Why `world` is a module and not a god aggregate**: the grid is a spatial
  substrate (a supporting subdomain — like a spatial database), not a
  business aggregate. It stays encapsulated behind a small public API;
  contexts never reach into its internals. Its rules (occupancy, scent
  evaporation) are still its own.
- **Cross-context mutation is forbidden**: `predators` never deletes an ant;
  `ants` never deletes a food source it emptied; `colony` never moves an ant.
  One-way effects on foreign aggregates arrive as **events** (`AntHatched`,
  `BirdAttacked`) which the owning module listens to and applies itself.
  Request/response operations (feeding, picking food up) stay synchronous
  commands to avoid turning the module graph into a cycle. This is what
  `ApplicationModules.verify()` enforces mechanically.
- **Sync vs async**: intra-tick coordination is synchronous module calls
  (ordered, deterministic, same thread). Async outbox events are used for
  facts that are *recorded* (history, metrics) or consumed outside the tick —
  never for the tick's own state changes (avoids races with the live
  in-memory world).
