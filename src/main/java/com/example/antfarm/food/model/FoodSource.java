package com.example.antfarm.food.model;

import com.example.antfarm.food.FoodId;
import com.example.antfarm.food.FoodType;
import com.example.antfarm.world.Position;

/**
 * One patch of food on the surface. Internal to the food module — the
 * public surface is {@code FoodService}.
 *
 * A source holds a finite amount of food; foragers draw it down until it is
 * depleted and removed. It emits food scent into the world so foragers can
 * smell it (handled by the service, not the aggregate).
 */
@com.example.ddd.DDDAggregateRoot
public final class FoodSource {

	private final FoodId id;
	private final Position position;
	private final FoodType type;
	private double amount;

	public FoodSource(FoodId id, Position position, FoodType type, double amount) {
		this.id = id;
		this.position = position;
		this.type = type;
		this.amount = amount;
	}

	public FoodId id() {
		return id;
	}

	public Position position() {
		return position;
	}

	public FoodType type() {
		return type;
	}

	public double amount() {
		return amount;
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
