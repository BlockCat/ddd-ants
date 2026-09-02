package com.example.antfarm.ants;

/**
 * Identity of a roaming adult ant, assigned by the ants context when the
 * ant is created.
 */
@com.example.ddd.DDDValueObject
public record AntId(long value) {

	@Override
	public String toString() {
		return "ant-" + value;
	}
}
