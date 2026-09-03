package com.example.antfarm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context smoke test backed by a real PostgreSQL Testcontainer.
 *
 * <p>The {@code disabledWithoutDocker} flag keeps the build green on
 * machines without a container runtime (e.g. this sandbox uses the local
 * PostgreSQL from apt): the test is skipped instead of failing there.
 */
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AntFarmApplicationTests {

	@Test
	void contextLoads() {
	}

}
