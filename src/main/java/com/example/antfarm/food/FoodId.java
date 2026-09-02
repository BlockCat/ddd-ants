package com.example.antfarm.food;

/**
 * Identity of a food source.
 */
@com.example.ddd.DDDValueObject
public record FoodId(long value) {

	@Override
	public String toString() {
		return "food-" + value;
	}
}
