package com.example.antfarm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full-context smoke test backed by the in-memory H2 database configured in
 * {@code src/test/resources/application.properties} — no external PostgreSQL
 * or container runtime required.
 */
@ActiveProfiles("test")
@SpringBootTest
class AntFarmApplicationTests {

	@Test
	void contextLoads() {
	}

}
