/**
 * World context — the sand terrain the simulation happens in.
 *
 * Owns the 2D grid of cells (sand, branches, other obstacles), the
 * occupancy of entities on it, and the pheromone/scent fields ants read
 * and write. Everything spatial (positions, movement, passability,
 * sensing) flows through this module.
 *
 * <p><b>Public API</b> — deliberately minimal: the {@link
 * com.example.antfarm.world.World} interface (the spatial substrate's
 * contract) plus the two value objects that appear in its signatures,
 * {@link com.example.antfarm.world.Position} and
 * {@link com.example.antfarm.world.TerrainKind}. The single implementation,
 * {@code WorldService}, lives in {@code world.internal} and is off-limits to
 * every other module: consumers program against the interface only.
 *
 * <p>Leaf module: nothing in the world depends on another module; every
 * other module depends on {@code World} for positions and grid access.
 * Business facts never leave the world as events — the world is a spatial
 * substrate (supporting subdomain), not a business context.
 */
@org.springframework.modulith.ApplicationModule
@com.example.ddd.DDDBoundedContext(name = "world", description = "Sand terrain, occupancy registry and pheromone field — the spatial substrate, exposed only through the World interface")
package com.example.antfarm.world;
