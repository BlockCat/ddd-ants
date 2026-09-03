package com.example.antfarm.food.internal;

import com.example.antfarm.food.FoodId;
import com.example.antfarm.food.FoodType;
import com.example.antfarm.world.Position;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A food source on the surface: where it lies, what it is made of, and how
 * much energy it still holds.
 *
 * A source holds a finite amount of food; foragers draw it down until it is
 * depleted and removed. It emits food scent into the world so foragers can
 * smell it (handled by the service, not the aggregate).
 */
@com.example.ddd.DDDAggregateRoot
@Accessors(fluent = true)
public final class FoodSource {

	@Getter
	private final FoodId id;
	@Getter
	private final Position position;
	@Getter
	private final FoodType type;
	@Getter
	private double amount;

	public FoodSource(FoodId id, Position position, FoodType type, double amount) {
		this.id = id;
		this.position = position;
		this.type = type;
		this.amount = amount;
	}

	public boolean isEmpty() {
		return amount <= 1e-9;
	}

	/** Takes up to {@code requested} food; returns what was actually taken. */
	public double takeUpTo(double requested) {
		double taken = Math.min(amount, requested);
		amount -= taken;
		return taken;
	}
}
