# Ubiquitous Language — Ant Farm Simulation

Terms used identically in code, docs and tests. If a term is missing, the
model is missing it. Forbidden synonyms reveal boundary seams.

| Term | Definition (domain words) | Code | Forbidden synonyms |
|---|---|---|---|
| Ant | One simulated insect; adult, belongs to a colony, has a role, a position, energy and a task. | `Ant` | bug*, critter, agent |
| Colony | The social unit: a nest, its queen, its brood and its stored food. | `Colony` | ant-farm, team |
| Nest | The colony's home: entrance hole on the surface plus chambers dug into the sand. | `Nest` | home*, hill (mound is a different shape) |
| Queen | The colony member that lays eggs; never leaves the nest. | `Queen` | mother |
| Worker | An adult ant that works around the nest: digs and clears sand, tends brood. | `Worker` | digger |
| Forager | An adult ant that leaves the nest to find food, carry it back and deposit it. | `Forager` | food-ant*, gatherer, scout |
| Brood | Eggs, larvae and pupae maturing inside the nest; need food to develop. | `Brood`, `Egg`, `Larva`, `Pupa` | babies |
| Stored food | The colony's food reserve kept in the nest; fed to brood and hungry ants. | `FoodStore` | inventory |
| Food source | A patch of edible matter (fruit, seed, carrion) lying on the surface with a finite amount. | `FoodSource` | food-pile, resource |
| Food scent | Pheromone foragers drop while carrying food and follow toward food; evaporates over time. | `ScentField` (food layer) | trail |
| Bird | A predator that catches ants found on open sand. | `Bird` | predator |
| Sand | The diggable, walkable ground of the world. | `TerrainCell(SAND)` | dirt, ground |
| Branch | A fallen branch; an obstacle ants cannot pass. | `TerrainCell(BRANCH)` | log, wood |
| Pebble | A small stone obstacle; ants cannot pass but it does not block birds. | `TerrainCell(PEBBLE)` | rock |
| Chamber | A dug-out cell of the nest where ants rest and brood is kept. | `Chamber` | room |
| Cell | One square of the world grid. | `Cell` | tile, pixel |
| Tick | One step of the simulation clock; all movement/decisions happen once per tick. | `tick` | frame, step |
| Energy | An ant's internal fuel; spent by acting, restored by eating stored food. | `energy` | hp, stamina |

*Bug* is fine for real insects but not our model; *food-ant* is ambiguous —
the demo treats food handling as the Forager role (confirm if you meant a
separate caste).
