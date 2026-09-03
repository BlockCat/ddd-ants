package com.example.antfarm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Structural guard for the bounded-context module layout.
 *
 * <p>Runs {@link ApplicationModules#verify()} so the build fails if a
 * context module reaches into another module's internals, if module
 * dependencies violate the declared {@code allowedDependencies}, or if
 * modules form cycles. This is a plain JUnit test — it must not boot the
 * Spring context ({@code @ApplicationModuleTest} is for module-scoped
 * integration tests, not for static verification).
 */
class ArchitectureTests {

	@Test
	void moduleStructureIsWellFormed() {
		ApplicationModules modules = ApplicationModules.of(AntFarmApplication.class);

		modules.verify();
		modules.forEach(System.out::println);

		List<String> names = new ArrayList<>();
		modules.forEach(module -> names.add(module.getIdentifier().toString()));

		assertEquals(
				List.of("ants", "colony", "food", "predators", "simulation", "world"),
				names.stream().sorted().toList(),
				"the six bounded contexts must each be their own application module");
	}

}
