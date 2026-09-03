package com.example.antfarm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Generates the Spring Modulith architecture documentation (component
 * diagrams as PlantUML + AsciiDoc) into
 * {@code src/main/resources/static/docs} so the running app can serve them
 * as pages.
 *
 * <p>The diagrams use {@code DiagramStyle.UML} — plain PlantUML component
 * syntax — rather than the default C4 style, so they render without the C4
 * PlantUML standard library (the C4 style emits {@code !include <C4/…>}
 * directives that fail when that library is not installed).
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
				.writeDocumentation(uml(), canvas())          // per-module AsciiDoc pages + diagrams
				.writeAggregatingDocument(uml(), canvas())    // overview of all modules
				.writeModulesAsPlantUml(uml())                // PlantUML of the module overview
				.writeIndividualModulesAsPlantUml(uml())      // one PlantUML per module
				.writeModuleCanvases(canvas())                // public API surface per module
				.writeModuleMetadata();
	}

	/** A fresh UML-style diagram configuration for one view (Structurizr keys views by it). */
	private static Documenter.DiagramOptions uml() {
		return Documenter.DiagramOptions.defaults()
				.withStyle(Documenter.DiagramOptions.DiagramStyle.UML);
	}

	private static Documenter.CanvasOptions canvas() {
		return Documenter.CanvasOptions.defaults();
	}
}
