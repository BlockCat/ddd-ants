# Domain Event Catalog

Events are business facts, past tense, value-only payloads (ids, positions,
amounts), each owned by the module where the fact occurs. **Per-tick movement
is state, not events** — events mark *significant* things: life-cycle,
sources appearing/disappearing, meals, attacks, dug chambers, and the
request/response traffic between domain contexts.

All events are published inside the tick transaction and delivered two ways:

1. **Synchronously** to `@EventListener` handlers in the reacting domain
   context — the effect lands in the same tick and thread (deterministic).
2. **Asynchronously** to `@ApplicationModuleListener` handlers (the
   `ANTFARM.EVENTS` logger in the simulation module) — Spring Modulith first
   writes the event to the `EVENT_PUBLICATION` outbox in the same
   transaction, so the fact is persisted before any async consumer runs.

## Events by owner (as implemented)

### `colony` (facts about the colony's life + meal/delivery traffic)
| Event | Meaning | Payload |
|---|---|---|
| `EggLaid` | Queen laid an egg because the colony is fed | `colonyId`, `tick` |
| `AntHatched` | Brood matured into an adult of a role | `colonyId`, `role`, `entrance`, `tick` |
| `FoodDeposited` | A forager delivery grew the store | `colonyId`, `antId` (raw long), `amount`, `storeAfter`, `tick` |
| `FoodConsumptionRequested` | A hungry ant asked the store for a meal | `colonyId`, `antId`, `amount`, `tick` |
| `FoodGranted` | The store granted that meal | `colonyId`, `antId`, `amount`, `tick` |
| `FoodDelivered` | A forager delivered carried food to the nest | `colonyId`, `antId`, `amount`, `tick` |

### `ants` (facts about individual adult ants)
| Event | Meaning | Payload |
|---|---|---|
| `AntDied` | An adult ant died, cause STARVED / OLD_AGE / EATEN | `antId`, `colonyId`, `cause`, `tick` |
| `ChamberDug` | A worker converted a sand cell into a chamber | `antId`, `colonyId`, `position`, `tick` |

(Starvation is folded into `AntDied(cause = STARVED)`; picking food up is
not announced separately by ants — the pickup and the delivery are the
meal-level facts, owned by food and colony respectively.)

### `food` (facts about food sources + pickup traffic)
| Event | Meaning | Payload |
|---|---|---|
| `FoodSourceSpawned` | A new source appeared on the terrain | `foodId`, `position`, `type`, `amount`, `tick` |
| `FoodSourceDepleted` | A source ran out and was removed | `foodId`, `position`, `tick` |
| `FoodPickupRequested` | A forager asked to pick food from a source | `antId` (raw long), `foodId`, `requested`, `tick` |
| `FoodPicked` | Food was actually taken from a source | `antId` (raw long), `foodId`, `amount`, `tick` |

### `predators` (facts about birds)
| Event | Meaning | Payload |
|---|---|---|
| `BirdAttacked` | A bird struck at an ant on open sand | `birdId`, `antId` (raw long), `position`, `tick` |

## Flow rules

1. **Owner publishes, others react — nobody else fires it.** `AntDied`
   (cause `EATEN`) is published by `ants`; `BirdAttacked` is published by
   `predators` as its own record of the strike.
2. **Domain contexts communicate only through events.** Meals, deliveries
   and pickups are request-events answered by response-events
   (`FoodConsumptionRequested → FoodGranted`, `FoodDelivered →
   FoodDeposited`, `FoodPickupRequested → FoodPicked`). The synchronous
   `@EventListener` handlers run in-thread, so the effect still lands in the
   same tick — no module mutates a foreign aggregate.
3. **Foreign effects are translated at the application layer.** A
   `BirdAttacked` fact is consumed by the `simulation` context, which calls
   `ants.kill(...)`; ants therefore never depends on predators.
4. **Payloads carry ids and values, never object references** — serialisable
   to the outbox and replayable. Ant ids are raw `long` on colony/food-owned
   events so those modules never depend on the ants module.
5. **Read models from events**: the ants context maintains its own view of
   food-source positions from `FoodSourceSpawned` / `FoodSourceDepleted`,
   instead of querying the food context.
6. **Persistence**: every event is consumed by an `@ApplicationModuleListener`
   (the `ANTFARM.EVENTS` logger), so Spring Modulith registers each one in
   the `EVENT_PUBLICATION` outbox during the tick transaction and delivers it
   after commit. Verify with:
   `select event_type, count(*), count(completion_date) from event_publication group by event_type;`
