package com.example.antfarm.colony.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.ColonyPolicy;
import com.example.antfarm.colony.Role;
import com.example.antfarm.world.Position;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The colony aggregate root: the nest at {@code entrance} with its queen,
 * its brood of eggs and its stored food.
 *
 * All colony rules live here: the queen lays an egg only when the store can
 * afford it, the brood cap is respected and a minimum time passes between
 * layings; hungry mouths draw from the store only while it is non-empty.
 * This class is internal to the colony module (no other module may touch
 * it) — the public surface is {@code ColonyService}.
 */
@com.example.ddd.DDDAggregateRoot
@Accessors(fluent = true)
public final class Colony {

	private static final double EPSILON = 1e-9;

	@Getter
	private final ColonyId id;
	@Getter
	private final Position entrance;
	@Getter
	private final Queen queen;
	private final ColonyPolicy policy;
	private final List<Egg> brood = new ArrayList<>();

	@Getter
	private double food;
	private long lastLayTick = Long.MIN_VALUE / 2;
	private int hatchTurn;

	/** Result of one colony tick: whether the queen laid and which castes matured. */
	public record ColonyTickResult(boolean eggLaid, List<Role> hatched) {
	}

	public Colony(ColonyId id, Position entrance, double initialFood, ColonyPolicy policy) {
		this.id = id;
		this.entrance = entrance;
		this.queen = new Queen(1);
		this.food = initialFood;
		this.policy = policy;
	}

	public int broodCount() {
		return brood.size();
	}

	/**
	 * One tick of colony life: the queen may lay, then the brood matures.
	 * Matured eggs are removed and their castes returned for hatching.
	 */
	public ColonyTickResult advance(long tick) {
		boolean laid = tryLay(tick);
		return new ColonyTickResult(laid, mature());
	}

	private boolean tryLay(long tick) {
		if (tick - lastLayTick < policy.layCooldownTicks()) {
			return false;
		}
		if (brood.size() >= policy.broodCap()) {
			return false;
		}
		if (food + EPSILON < policy.eggCost()) {
			return false; // queen is not fed enough
		}
		food -= policy.eggCost();
		brood.add(new Egg(policy.eggTicks()));
		lastLayTick = tick;
		return true;
	}

	private List<Role> mature() {
		List<Role> hatched = new ArrayList<>();
		for (Iterator<Egg> it = brood.iterator(); it.hasNext();) {
			if (it.next().advance()) {
				it.remove();
				hatched.add(nextRole());
			}
		}
		return hatched;
	}

	/** Round-robin balance between workers and foragers. */
	private Role nextRole() {
		return hatchTurn++ % 2 == 0 ? Role.WORKER : Role.FORAGER;
	}

	/** Withdraws food if enough is stored; returns whether it succeeded. */
	public boolean tryConsume(double amount) {
		if (food + EPSILON < amount) {
			return false;
		}
		food -= amount;
		return true;
	}

	public void deposit(double amount) {
		food += amount;
	}
}
