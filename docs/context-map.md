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
| `food` | `ants` | Customer–supplier: foragers draw food down at a source | Synchronous call in tick (forager → `food.pickUp(sourceId, amount)`) | `ants` |
| `colony` | `ants` | Customer–supplier: ants deposit carried food, feed from the store, learn nest entrance | Synchronous public API of `colony` | `ants` |
| `colony` | `ants` | New adults announced | Domain event `AntHatched` (colony owns it) | `colony` publishes, engine spawns |
| `predators` | `ants` | Bird attacks become ant deaths | Engine-mediated: `predators` reports attacks → engine calls `ants` → `ants` publishes `AntDied` | `simulation` (mediator) |
| all contexts | `simulation` | Significant facts recorded / observable | Domain events → outbox (event publication registry) → async listeners append history | publisher of each event |

## Notes on the seams

- **Why `world` is a module and not a god aggregate**: the grid is a spatial
  substrate (a supporting subdomain — like a spatial database), not a
  business aggregate. It stays encapsulated behind a small public API;
  contexts never reach into its internals. Its rules (occupancy, scent
  evaporation) are still its own.
- **Cross-context mutation is forbidden**: `predators` never deletes an ant;
  `ants` never deletes a food source it emptied; `colony` never moves an ant.
  Effects on foreign aggregates go through the engine mediator or the owning
  module's public API. This is what `ApplicationModules.verify()` enforces
  mechanically.
- **Sync vs async**: intra-tick coordination is synchronous module calls
  (ordered, deterministic, same thread). Async outbox events are used for
  facts that are *recorded* (history, metrics) or consumed outside the tick —
  never for the tick's own state changes (avoids races with the live
  in-memory world).
