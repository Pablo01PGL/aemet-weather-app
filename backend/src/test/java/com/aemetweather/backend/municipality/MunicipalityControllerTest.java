package com.aemetweather.backend.municipality;

import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MunicipalityController.class)
class MunicipalityControllerTest {

	@Autowired
	private MockMvcTester mockMvcTester;

	@MockitoBean
	private MunicipalityService municipalityService;

	@Test
	void search_withResults_returns200AndBody() {
		when(municipalityService.search("abad")).thenReturn(List.of(
			new MunicipalityResponse("44001", "Ababuj"),
			new MunicipalityResponse("10001", "Abadía")));

		mockMvcTester.get().uri("/api/municipalities").param("name", "abad")
			.assertThat()
			.hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
				[
				  {"code": "44001", "name": "Ababuj"},
				  {"code": "10001", "name": "Abadía"}
				]
				""");
	}

	@Test
	void search_withNoResults_returns200AndEmptyArray() {
		when(municipalityService.search("zzz")).thenReturn(List.of());

		mockMvcTester.get().uri("/api/municipalities").param("name", "zzz")
			.assertThat()
			.hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("[]");
	}

	@Test
	void search_withMissingNameParam_returns400() {
		mockMvcTester.get().uri("/api/municipalities")
			.assertThat()
			.hasStatus4xxClientError()
			.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void search_withBlankNameParam_returns400() {
		mockMvcTester.get().uri("/api/municipalities").param("name", "   ")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST);
	}
}
