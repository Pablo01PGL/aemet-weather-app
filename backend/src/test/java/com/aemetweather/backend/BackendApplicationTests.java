package com.aemetweather.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "aemet.api-key=dummy-test-key")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
