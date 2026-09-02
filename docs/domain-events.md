# Domain Event Catalog

Events are business facts, past tense, value-only payloads (ids, positions,
amounts), each owned by the module where the fact occurs. **Per-tick movement
is state, not events** — events mark *significant* things: life-cycle,
sources appearing/disappearing, meals, attacks, dug chambers.

## Events by owner (as implemented)

### `colony` (facts about the colony's life)
| Event | Meaning | Payload |
|---|---|---|
| `EggLaid` | Queen laid an egg because the colony is fed | `colonyId`, `tick` |
| `AntHatched` | Brood matured into an adult of a role | `colonyId`, `role`, `entrance`, `tick` |
| `FoodDeposited` | A forager delivered carried food into the store | `colonyId`, `antId` (raw long), `amount`, `storeAfter`, `tick` |

### `ants` (facts about individual adult ants)
| Event | Meaning | Payload |
|---|---|---|
| `AntDied` | An adult ant died, cause STARVED / OLD_AGE / EATEN | `antId`, `colonyId`, `cause`, `tick` |
| `ChamberDug` | A worker converted a sand cell into a chamber | `antId`, `colonyId`, `position`, `tick` |

(Starvation is folded into `AntDied(cause = STARVED)`; picking food up is
not announced separately — the deposit is the meal-level fact.)

### `food` (facts about food sources)
| Event | Meaning | Payload |
|---|---|---|
| `FoodSourceSpawned` | A new source appeared on the terrain | `foodId`, `position`, `type`, `amount`, `tick` |
| `FoodSourceDepleted` | A source ran out and was removed | `foodId`, `position`, `tick` |

### `predators` (facts about birds)
| Event | Meaning | Payload |
|---|---|---|
| `BirdAttacked` | A bird struck at an ant on open sand | `birdId`, `antId`, `position`, `tick` |

## Flow rules

1. **Owner publishes, others react — nobody else fires it.** `AntDied`
   (cause `EATEN`) is published by `ants`; `BirdAttacked` is published by
   `predators` as its own record of the strike.
2. **Cross-module effects are synchronous in the tick** (engine-mediated):
   the engine takes `BirdAttack`s from the predators module and applies them
   as `ants.kill(...)`; colony hatch requests become `ants.spawn(...)`. The
   simulation stays deterministic and no module mutates a foreign aggregate.
3. **Payloads carry ids and values, never object references** — serialisable
   to the outbox and replayable.
4. **Recorded, not reacted-to-synchronously**: `SimulationEventLogger` (in
   the simulation module) consumes every event via
   `@ApplicationModuleListener` — async, after commit — and only logs.
5. **Delivery**: events are published inside the tick transaction → Spring
   Modulith writes them to the `EVENT_PUBLICATION` outbox table → the async
   listeners run after commit and the publication is marked completed.
   Verify with:
   `select event_type, count(*), count(completion_date) from event_publication group by event_type;`
