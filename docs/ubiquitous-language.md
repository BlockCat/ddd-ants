# Ubiquitous Language — Ant Farm Simulation

Terms used identically in code, docs and tests. If a term is missing, the
model is missing it. Forbidden synonyms reveal boundary seams.

| Term | Definition (domain words) | Code | Forbidden synonyms |
|---|---|---|---|
| Ant | One simulated insect; adult, belongs to a colony, has a role, a position, energy, a load and momentum. | `Ant` | bug*, critter, agent |
| Colony | The social unit: a nest, its queen, its brood and its stored food. | `Colony` | ant-farm, team |
| Nest | The colony's home: entrance hole on the surface plus chambers dug into the sand. | `Colony` (entrance) | home*, hill (mound is a different shape) |
| Queen | The colony member that lays eggs; never leaves the nest. | `Queen` | mother |
| Worker | An adult ant that works around the nest: digs and clears sand, tends brood. | `Role.WORKER` | digger |
| Forager | An adult ant that leaves the nest to find food, carry it back and deliver it. | `Role.FORAGER` | food-ant*, gatherer, scout |
| Brood | Eggs, larvae and pupae maturing inside the nest; need food to develop. | `Egg` | babies |
| Stored food | The colony's food reserve kept in the nest; fed to brood and hungry ants. | `Colony.food` | inventory |
| Food source | A patch of edible matter (fruit, seed, carrion) lying on the surface with a finite amount. | `FoodSource` | food-pile, resource |
| Food scent | Pheromone foragers drop while carrying food and follow toward food; evaporates over time. | `World` food scent field | trail |
| Momentum | The unit direction an ant keeps walking; makes paths smooth (straight ≫ sideways ≫ U-turn). | `Momentum` | heading, velocity |
| Bird | A predator that catches ants found on open sand. | `Bird` | predator |
| Sand | The diggable, walkable ground of the world. | `TerrainKind.SAND` | dirt, ground |
| Branch | A fallen branch; an obstacle ants cannot pass. | `TerrainKind.BRANCH` | log, wood |
| Pebble | A small stone obstacle; ants cannot pass but it does not block birds. | `TerrainKind.PEBBLE` | rock |
| Chamber | A dug-out cell of the nest where ants rest and brood is kept. | `TerrainKind.CHAMBER` | room |
| Cell | One square of the world grid. | `Position` / grid cell | tile, pixel |
| Tick | One step of the simulation clock; all movement/decisions happen once per tick. | `tick` | frame, step |
| Energy | An ant's internal fuel; spent by acting, restored by eating stored food. | `Ant.energy` | hp, stamina |
| Meal | Food withdrawn from the colony store to feed a hungry ant. | `FoodConsumptionRequested` / `FoodGranted` | snack |
| Delivery | Carried food a forager brings home into the colony store. | `FoodDelivered` / `FoodDeposited` | drop-off |

*Bug* is fine for real insects but not our model; *food-ant* is ambiguous —
the demo treats food handling as the Forager role (confirm if you meant a
separate caste). *Heading* and *velocity* are folded into *Momentum* — one
word, one value object.
