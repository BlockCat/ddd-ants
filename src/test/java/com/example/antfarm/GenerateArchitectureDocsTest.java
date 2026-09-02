package com.example.antfarm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Generates the Spring Modulith architecture documentation (C4-style
 * component diagrams as PlantUML + AsciiDoc) into
 * {@code src/main/resources/static/docs} so the running app can serve them
 * as pages.
 *
 * <p>Run explicitly with:
 * {@code ./mvnw test -Dtest=GenerateArchitectureDocsTest}
 * (also runs as part of the full test suite — generation is idempotent).
 */
class GenerateArchitectureDocsTest {

	@Test
	void generateArchitectureDiagrams() {
		ApplicationModules modules = ApplicationModules.of(AntFarmApplication.class);
		modules.verify();

		Documenter.Options options = Documenter.Options.defaults()
				.withOutputFolder("src/main/resources/static/docs");
		Documenter documenter = new Documenter(modules, options);

		documenter
				.writeDocumentation()              // per-module AsciiDoc pages + diagrams
				.writeAggregatingDocument()        // overview of all modules
				.writeModulesAsPlantUml()          // PlantUML of the module overview
				.writeIndividualModulesAsPlantUml()
				.writeModuleCanvases()             // public API surface per module
				.writeModuleMetadata();
	}
}
