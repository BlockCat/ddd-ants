package com.example.antfarm.colony.model;

/**
 * The colony's queen. In this increment she is a fixed member of the nest
 * aggregate whose only behaviour is expressed through the colony policy
 * (she lays eggs while the store is fed); she never leaves the nest.
 */
@com.example.ddd.DDDEntity
public record Queen(long id) {
}
