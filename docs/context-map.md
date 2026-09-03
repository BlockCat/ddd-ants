# Context Map — Ant Farm Simulation

Six bounded contexts, each a Spring Modulith application module. The core
domain is **ants** (the behaviour rules that make the simulation live);
**colony** is the second core context; the **world** is the spatial substrate
everything depends on.

## Contexts

| Context (module) | Type | Responsibility | Language highlights |
|---|---|---|---|
| `world` | Supporting (spatial substrate) | Grid of terrain cells (sand/branch/pebble/chamber), occupancy of entities, pheromone/scent fields, passability & sensing. Exposed **only** through the `World` interface. | Cell, Sand, Branch, Position |
| `colony` | Core | Nest aggregate: queen, brood, stored food, entrance & chambers; policies: lay eggs when fed, mature brood into needed role. | Nest, Queen, Brood, FoodStore |
| `ants` | Core | Free-roaming adults: role behaviour, energy, momentum, tasks (forage, dig, rest, return, deliver), life & death. | Ant, Worker, Forager, Momentum |
| `food` | Supporting | Food sources: amount, depletion, respawn policy; food scent emission. | FoodSource |
| `predators` | Supporting | Birds: patrol, hunger, hunting surface ants. | Bird |
| `simulation` | Generic/application (not domain) | Clock, tick cadence, tick order, cross-context mediation, event publication to outbox, snapshot for rendering. | Tick |

## Communication rules (the two hard rules of this design)

1. **Domain contexts talk to each other only through events.** No domain
   module (`colony`, `ants`, `food`, `predators`) calls another domain
   module's service. Requests, grants and facts are all events, published
   inside the tick transaction and persisted by the Spring Modulith event
   publication registry (outbox) before async delivery.
2. **The world is the one exception**: it is a leaf spatial substrate
   (like a spatial database), accessed synchronously and **only** through its
   `World` interface — never through its internals or concrete
   `WorldService`.

## Relationships (module dependencies = declared `allowedDependencies`)

```
simulation ──► colony ──► world ◄── food ◄── ants
   │  └────► ants ──► colony (events only)   ▲
   │            └────► food (events only)    │
   └────────► food                           │
   └────────► predators ──► world ───────────┘
        │
        └── (BirdAttacked) ──► simulation mediates ──► ants.kill()
```

| Upstream | Downstream | Relationship | Mechanism | Translation owner |
|---|---|---|---|---|
| `world` | everyone | Customer–supplier: world supplies space & sensing; consumers adapt to its grid API | Synchronous calls on the **`World` interface** (position, passability, scent, occupancy) | Consumers |
| `colony` | `ants` | New adults announced | Event `AntHatched` → ants spawns the adult | `colony` publishes, `ants` listens |
| `ants` | `colony` | Hunger → meal; arrival → delivery | Request-events `FoodConsumptionRequested` / `FoodDelivered`; colony answers with `FoodGranted` / `FoodDeposited` | `ants` publishes requests, `colony` owns the store |
| `ants` | `food` | Forager picks food up | Request-event `FoodPickupRequested`; food answers with `FoodPicked` / `FoodSourceDepleted` | `ants` publishes requests, `food` owns sources |
| `food` | `ants` | Source positions for foraging | Events `FoodSourceSpawned` / `FoodSourceDepleted` feed the ants context's local read model | `food` publishes, `ants` projects |
| `predators` | `ants` | Bird strikes become ant deaths | Event `BirdAttacked` → **simulation mediates** → `ants.kill(...)` → `AntDied(EATEN)` | `simulation` (ants never depends on predators) |
| all contexts | `simulation` | Significant facts recorded / observable | Events → outbox (event publication registry) → async listeners | publisher of each event |

## Notes on the seams

- **Why `world` is a module and not a god aggregate**: the grid is a spatial
  substrate (a supporting subdomain — like a spatial database), not a
  business aggregate. It stays encapsulated behind the `World` interface;
  contexts never reach into its internals.
- **Cross-context mutation is forbidden**: `predators` never deletes an ant;
  `ants` never deletes a food source it emptied; `colony` never moves an ant.
  Effects on foreign aggregates arrive as events, which the owning module
  listens to and applies itself. The one fact that spans contexts
  (bird strikes) is translated by the application layer (`simulation`) so
  that `ants` never has to know about `predators`.
- **Request/response is modelled as request-events + response-events** —
  the event-driven alternative to synchronous command calls. State changes
  still land in the same tick and thread (synchronous `@EventListener`),
  while the same events are persisted and observed asynchronously by
  `@ApplicationModuleListener` consumers. `ApplicationModules.verify()`
  enforces the declared dependency directions mechanically.
