package com.example.antfarm.predators;

/**
 * Identity of a bird.
 */
@com.example.ddd.DDDValueObject
public record BirdId(long value) {

	@Override
	public String toString() {
		return "bird-" + value;
	}
}
