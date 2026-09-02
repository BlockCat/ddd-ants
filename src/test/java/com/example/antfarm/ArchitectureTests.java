package com.example.antfarm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Structural guard for the bounded-context module layout.
 *
 * Fails the build if a context module reaches into another module's
 * internals, if module dependencies violate the declared
 * {@code allowedDependencies}, or if modules form cycles. Run in CI; keep
 * green before adding cross-module references.
 */
class ArchitectureTests {

	@Test
	void moduleStructureIsWellFormed() {
		ApplicationModules modules = ApplicationModules.of(AntFarmApplication.class);
		modules.verify();
		modules.forEach(System.out::println);
	}

}
