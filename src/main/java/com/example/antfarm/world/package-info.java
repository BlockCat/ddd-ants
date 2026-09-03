/**
 * World context — the sand terrain the simulation happens in.
 *
 * Owns the 2D grid of cells (sand, branches, other obstacles), the
 * occupancy of entities on it, and the pheromone/scent fields ants read
 * and write. Everything spatial (positions, movement, passability,
 * sensing) flows through this module.
 *
 * Leaf module: nothing in the domain depends on it; most other modules
 * depend on it for {@link com.example.antfarm.world.Position} and grid access.
 */
@org.springframework.modulith.ApplicationModule
@com.example.ddd.DDDBoundedContext(name = "world", description = "Sand terrain, occupancy registry and pheromone field — the spatial substrate")
package com.example.antfarm.world;
