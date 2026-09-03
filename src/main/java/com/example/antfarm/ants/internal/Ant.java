package com.example.antfarm.ants.internal;

import com.example.antfarm.ants.AntId;
import com.example.antfarm.colony.ColonyId;
import com.example.antfarm.colony.Role;
import com.example.antfarm.world.Position;

/**
 * One roaming adult ant. Internal to the ants module; the public surface is
 * {@code AntService}.
 *
 * An ant is either {@code inside} its nest (no grid position — it is not
 * registered in the world) or {@code outside} at a grid {@code position}.
 * While outside it is either {@code EXPLORING} (random walk) or
 * {@code RETURNING} home; energy is spent by acting and restored by meals
 * taken from the colony store while inside.
 */
@com.example.ddd.DDDAggregateRoot
public final class Ant {

	public enum Activity {
		EXPLORING,
		RETURNING
	}

	private final AntId id;
	private final ColonyId colonyId;
	private final Role role;
	private final Position entrance;

	private boolean inside = true;
	private Position position; // null while inside
	private double energy;
	private double carrying;
	private Activity activity = Activity.EXPLORING;
	private long ticksOutside;
	private long exploreBudget;
	private int insideTicks;
	private int headingX = 0;
	private int headingY = -1; // last direction of travel (unit vector, north default)

	public Ant(AntId id, ColonyId colonyId, Role role, Position entrance, double startEnergy) {
		this.id = id;
		this.colonyId = colonyId;
		this.role = role;
		this.entrance = entrance;
		this.energy = startEnergy;
		this.carrying = 0;
	}

	public AntId id() {
		return id;
	}

	public ColonyId colonyId() {
		return colonyId;
	}

	public Role role() {
		return role;
	}

	public Position entrance() {
		return entrance;
	}

	public boolean isInside() {
		return inside;
	}

	public Position position() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public int headingX() {
		return headingX;
	}

	public int headingY() {
		return headingY;
	}

	/** Records the direction of travel (a unit step: dx/dy in -1..1). */
	public void setHeading(int dx, int dy) {
		if (dx != 0 || dy != 0) {
			this.headingX = Integer.compare(dx, 0);
			this.headingY = Integer.compare(dy, 0);
		}
	}

	public double energy() {
		return energy;
	}

	public double carrying() {
		return carrying;
	}

	public void addCarrying(double amount) {
		this.carrying += amount;
	}

	/** Takes all carried food; caller deposits it. */
	public double takeCarrying() {
		double carried = this.carrying;
		this.carrying = 0;
		return carried;
	}

	public Activity activity() {
		return activity;
	}

	public boolean isReturning() {
		return activity == Activity.RETURNING;
	}

	public long ticksOutside() {
		return ticksOutside;
	}

	public long exploreBudget() {
		return exploreBudget;
	}

	public int insideTicks() {
		return insideTicks;
	}

	public void markOutsideTick() {
		ticksOutside++;
	}

	/** Leaves the nest onto {@code spot} (must already be registered in the world). */
	public void leaveNest(Position spot, long budget) {
		this.inside = false;
		this.position = spot;
		this.activity = Activity.EXPLORING;
		this.ticksOutside = 0;
		this.insideTicks = 0;
		this.exploreBudget = budget;
	}

	/** Arrives home; position is released by the caller in the world registry. */
	public void enterNest() {
		this.inside = true;
		this.position = null;
		this.activity = Activity.EXPLORING;
		this.ticksOutside = 0;
		this.insideTicks = 0;
	}

	public void startReturningHome() {
		this.activity = Activity.RETURNING;
	}

	public void spend(double amount) {
		this.energy -= amount;
	}

	public void refill(double amount) {
		this.energy += amount;
	}

	public void tickInside() {
		this.insideTicks++;
	}

	@Override
	public String toString() {
		return "%s(%s, %s, %s, inside=%s, e=%.1f)"
				.formatted(id, role, colonyId, position != null ? position : "nest", inside, energy);
	}
}
