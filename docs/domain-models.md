# Domain Models — Ant Farm Simulation

The tactical model per bounded context. One module per context; every
aggregate/entity/value object below is findable in code under
`src/main/java/com/example/antfarm/<context>`.

Legend: **AR** = aggregate root, **E** = entity, **VO** = value object,
**EV** = domain event, **CMD** = command. Internals live in
`<context>/internal` and are invisible across module boundaries (enforced by
`ApplicationModules.verify()`).

## `world` — spatial substrate (supporting subdomain)

The world is *not* a business aggregate. It is an in-process spatial database
whose public surface is exactly one interface plus two value objects.

| Type | Element | Where | Notes / invariants |
|---|---|---|---|
| port | `World` | `world/World.java` | The only service contract consumers may use |
| impl | `WorldService` | `world/internal/WorldService.java` | `@Service`, implements `World`; not visible to other modules |
| VO | `Position` | `world/Position.java` | `(x, y)`, non-negative; neighbours, manhattan distance |
| VO | `TerrainKind` | `world/TerrainKind.java` | SAND, BRANCH, PEBBLE, HOLE, TUNNEL, CHAMBER; walkable/underground rules |

Rules: terrain changes only through `World` operations; occupancy is
mutually exclusive per cell; the burrow grows only connected to the nest
(`digChamber`/`digTunnel`/`openHole` all require an adjacent burrow cell).

## `colony` — nest aggregate (core domain)

| Type | Element | Where | Notes / invariants |
|---|---|---|---|
| AR | `Colony` | `colony/internal/Colony.java` | nest = entrance + queen + brood + food store |
| E | `Queen` | `colony/internal/Queen.java` | fixed member; never leaves the nest |
| E | `Egg` | `colony/internal/Egg.java` | matures after `eggTicks` |
| VO | `ColonyId` | `colony/ColonyId.java` | typed id |
| VO | `ColonyPolicy` | `colony/ColonyPolicy.java` | egg cost, egg ticks, brood cap, lay cooldown |
| VO | `Role` | `colony/Role.java` | WORKER, FORAGER (the caste vocabulary) |
| EV | `EggLaid` | `colony/EggLaid.java` | queen laid because the store could afford it |
| EV | `AntHatched` | `colony/AntHatched.java` | brood matured into a role |
| EV | `FoodDeposited` | `colony/FoodDeposited.java` | store grew by a delivery |
| EV | `FoodConsumptionRequested` | `colony/FoodConsumptionRequested.java` | a hungry ant asked for a meal (request-event) |
| EV | `FoodGranted` | `colony/FoodGranted.java` | the store granted a meal (response-event) |
| EV | `FoodDelivered` | `colony/FoodDelivered.java` | a forager delivered carried food (request-event) |

Invariants (all in `Colony`): lay only while `food >= eggCost` and under the
brood cap with the cooldown respected; a meal is granted only while the
store can cover it.

## `ants` — roaming-adult aggregate (core domain)

| Type | Element | Where | Notes / invariants |
|---|---|---|---|
| AR | `Ant` | `ants/internal/Ant.java` | identity, colony, role, position, energy, load, activity, momentum |
| VO | `AntId` | `ants/AntId.java` | typed id |
| VO | `Momentum` | `ants/Momentum.java` | unit direction of travel (`dx`,`dy` in -1..1) |
| VO | `AntPolicy` | `ants/AntPolicy.java` | behaviour knobs (energy costs, sniff radius, dig probability …) |
| CMD | `SpawnAnt` | `ants/SpawnAnt.java` | bring a new adult into the world |
| EV | `AntDied` | `ants/AntDied.java` | cause STARVED / OLD_AGE / EATEN |
| EV | `ChamberDug` | `ants/ChamberDug.java` | a worker carved a chamber |

Invariants: an ant is either `inside` (no grid position) or `outside` at a
position registered in the world; energy and carried load are changed only
through behaviour (`spend`, `refill`, `addCarrying`, `takeCarrying`); state
transitions (`leaveNest`, `enterNest`, `startReturningHome`) are behaviour,
not setters.

## `food` — food-source aggregate (supporting subdomain)

| Type | Element | Where | Notes / invariants |
|---|---|---|---|
| AR | `FoodSource` | `food/internal/FoodSource.java` | position, type, finite amount |
| VO | `FoodId` | `food/FoodId.java` | typed id |
| VO | `FoodType` | `food/FoodType.java` | FRUIT, SEED, CARRION |
| EV | `FoodSourceSpawned` | `food/FoodSourceSpawned.java` | a new source appeared |
| EV | `FoodSourceDepleted` | `food/FoodSourceDepleted.java` | a source ran out and was removed |
| EV | `FoodPickupRequested` | `food/FoodPickupRequested.java` | a forager asked to pick food (request-event) |
| EV | `FoodPicked` | `food/FoodPicked.java` | food was actually taken (response-event) |

Invariant: a source is removed the moment it becomes empty.

## `predators` — bird entity (supporting subdomain)

| Type | Element | Where | Notes / invariants |
|---|---|---|---|
| E | `Bird` | `predators/internal/Bird.java` | position, hunt cooldown |
| VO | `BirdId` | `predators/BirdId.java` | typed id |
| EV | `BirdAttacked` | `predators/BirdAttacked.java` | a bird struck at an ant (the fact only) |

Invariant: after a strike the bird cannot hunt again until its cooldown
passes. The *effect* of the strike (the ant's death) is applied by the
`ants` context via the `simulation` mediator — predators never mutates ants.

## `simulation` — application layer (not a domain context)

No domain model. It owns the tick clock, the fixed tick order, the
translation of foreign facts into owning-context commands, the snapshot view
model, and the web adapter. Public: `SimulationEngine`, `SimulationSnapshot`,
`SimulationApiController`; everything else is `simulation/internal`.
