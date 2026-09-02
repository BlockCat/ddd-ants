package com.example.antfarm.colony;

/**
 * Identity of a colony. Colonies are created by the engine; ids are unique
 * per run.
 */
@com.example.ddd.DDDValueObject
public record ColonyId(long value) {

	@Override
	public String toString() {
		return "colony-" + value;
	}
}
