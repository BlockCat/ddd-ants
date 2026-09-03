/**
 * Food context — food sources in the world.
 *
 * Owns the {@code FoodSource} aggregate (fruit, seed, carrion …): where it
 * lies, how much energy it holds, and how foraging ants draw it down until
 * it is depleted. A spawner policy keeps a steady supply of sources
 * appearing on the terrain.
 *
 * Depends on {@code world} for placing sources on free sand cells and for
 * emitting the food scent that guides foragers. The aggregate lives in
 * {@code food.internal}.
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = "world")
@com.example.ddd.DDDBoundedContext(name = "food", description = "Food sources that spawn, emit scent, are eaten down and deplete")
package com.example.antfarm.food;
