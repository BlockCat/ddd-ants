# Knowledge Map — Ant Farm Simulation

A single index of the domain knowledge: every ubiquitous-language term, the
bounded context that owns it, its model element, and its home in the code.
Use it alongside `ubiquitous-language.md` (definitions) and
`context-map.md` (boundaries).

## Reading the map

- **Owner** — the bounded context where the term is *defined and enforced*.
- **Kind** — subdomain type: *core* (the behaviour that makes the sim live),
  *supporting* (needed but not differentiating), *generic/application*
  (orchestration, web, infrastructure).
- **Element** — DDD building block: AR (aggregate root), E (entity),
  VO (value object), EV (event), CMD (command), Port (interface), Service.

| Term | Owner | Kind | Element | Code |
|---|---|---|---|---|
| World / terrain | world | supporting | Port | `world/World.java` |
| Cell | world | supporting | concept | `world/World.java` (grid) |
| Sand, Branch, Pebble, Hole, Tunnel, Chamber | world | supporting | VO | `world/TerrainKind.java` |
| Position | world | supporting | VO | `world/Position.java` |
| Food scent / pheromone | world | supporting | concept | `World.emitFoodScent` / `foodScentAt` / `advanceScent` |
| Colony / nest | colony | core | AR | `colony/internal/Colony.java` |
| Queen | colony | core | E | `colony/internal/Queen.java` |
| Brood / egg | colony | core | E | `colony/internal/Egg.java` |
| Stored food | colony | core | field | `Colony.food` |
| Colony policy | colony | core | VO | `colony/ColonyPolicy.java` |
| Role / caste | colony | core | VO | `colony/Role.java` |
| Lay egg | colony | core | rule | `Colony.tryLay` |
| Hatch | colony | core | EV | `colony/AntHatched.java` |
| Ant | ants | core | AR | `ants/internal/Ant.java` |
| Ant id | ants | core | VO | `ants/AntId.java` |
| Momentum / heading | ants | core | VO | `ants/Momentum.java` |
| Energy | ants | core | field | `Ant.energy` |
| Carried food / load | ants | core | field | `Ant.carrying` |
| Forage | ants | core | behaviour | `AntService.followFoodScent` / `advanceExploring` |
| Dig | ants | core | behaviour | `AntService.tryDig` |
| Die | ants | core | EV | `ants/AntDied.java` |
| Food source | food | supporting | AR | `food/internal/FoodSource.java` |
| Food type | food | supporting | VO | `food/FoodType.java` |
| Food id | food | supporting | VO | `food/FoodId.java` |
| Spawn source | food | supporting | EV | `food/FoodSourceSpawned.java` |
| Deplete source | food | supporting | EV | `food/FoodSourceDepleted.java` |
| Bird | predators | supporting | E | `predators/internal/Bird.java` |
| Bird id | predators | supporting | VO | `predators/BirdId.java` |
| Bird attack / strike | predators | supporting | EV | `predators/BirdAttacked.java` |
| Tick | simulation | generic/application | concept | `SimulationEngine.tick` |
| Snapshot | simulation | generic/application | VO/view | `simulation/SimulationSnapshot.java` |
| Request a meal | colony (receiver) | — | EV | `colony/FoodConsumptionRequested.java` |
| Grant a meal | colony (sender) | — | EV | `colony/FoodGranted.java` |
| Deliver food | colony (receiver) | — | EV | `colony/FoodDelivered.java` |
| Request a pickup | food (receiver) | — | EV | `food/FoodPickupRequested.java` |
| Pick food | food (sender) | — | EV | `food/FoodPicked.java` |

## Knowledge flow (who knows what)

```
world      knows: terrain, occupancy, scent.               knows nothing else.
colony     knows: queen, brood, store, roles.              reacts to: meal requests, deliveries.
ants       knows: ant state + behaviour.                   reacts to: hatches, grants, pickups, source facts.
food       knows: sources.                                 reacts to: pickup requests.
predators  knows: birds.                                   reacts to: nothing foreign.
simulation knows: the tick order + every public contract.  mediates: predator strikes → ant deaths.
```

## Rules that have exactly one home

1. **A cell is walkable/underground/a hole** → `TerrainKind`.
2. **Movement respects surface/underground boundaries (only through a hole)** → `World.movementAllowed`.
3. **The burrow grows only connected to the nest** → `World.digChamber`/`digTunnel`/`openHole`.
4. **The queen lays only when fed, under the brood cap, after the cooldown** → `Colony.tryLay`.
5. **A meal is granted only while the store covers it** → `Colony.tryConsume`.
6. **An ant prefers straight ahead ≫ sideways ≫ U-turn** → `AntService.headingWeight` (with `Momentum`).
7. **A source is removed the moment it is empty** → `FoodSource.takeUpTo` + `FoodService.take`.
8. **A bird cannot hunt again until its cooldown passes** → `Bird.armHunt`/`readyToHunt`.
