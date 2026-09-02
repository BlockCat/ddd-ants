package com.example.antfarm.world;

/**
 * A cell coordinate in the ant world.
 *
 * The one value object every context shares: positions of nests, food
 * sources, birds and ants are all {@code Position}s, and the world grid is
 * indexed by them. Immutable; equality by coordinates.
 *
 * @param x column index, {@code 0 <= x < width}
 * @param y row index, {@code 0 <= y < height}
 */
@com.example.ddd.DDDValueObject
public record Position(int x, int y) {

	public Position {
		if (x < 0 || y < 0) {
			throw new IllegalArgumentException("Position cannot be negative: (%d,%d)".formatted(x, y));
		}
	}

	/** The (non-negative) orthogonal neighbours; callers check validity against the world. */
	public java.util.List<Position> neighbours() {
		java.util.List<Position> result = new java.util.ArrayList<>(4);
		addIfValid(result, x + 1, y);
		addIfValid(result, x - 1, y);
		addIfValid(result, x, y + 1);
		addIfValid(result, x, y - 1);
		return result;
	}

	private static void addIfValid(java.util.List<Position> result, int nx, int ny) {
		if (nx >= 0 && ny >= 0) {
			result.add(new Position(nx, ny));
		}
	}

	/** Manhattan distance to another cell — the metric ants actually walk. */
	public int distanceTo(Position other) {
		return Math.abs(x - other.x) + Math.abs(y - other.y);
	}

	@Override
	public String toString() {
		return "(%d,%d)".formatted(x, y);
	}
}
