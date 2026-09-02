package com.example.antfarm.ants;

import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.Role;
import com.example.antfarm.world.Position;

/**
 * Command to bring a new adult ant into the world (from colony brood or the
 * engine's initial seeding). The ant starts inside its nest at
 * {@code entrance}.
 */
@com.example.ddd.DDDCommand
public record SpawnAnt(ColonyId colonyId, Role role, Position entrance) {
}
