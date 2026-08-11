package com.aemetweather.backend.exception;

import static org.mockito.Mockito.when;

import com.aemetweather.backend.forecast.ForecastController;
import com.aemetweather.backend.forecast.ForecastService;
import com.aemetweather.backend.forecast.TemperatureUnit;
import com.aemetweather.backend.municipality.MunicipalityController;
import com.aemetweather.backend.municipality.MunicipalityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(controllers = {MunicipalityController.class, ForecastController.class})
class GlobalExceptionHandlerTest {

	private static final String MUNICIPALITY_CODE = "44001";

	@Autowired
	private MockMvcTester mockMvcTester;

	@MockitoBean
	private MunicipalityService municipalityService;

	@MockitoBean
	private ForecastService forecastService;

	@Test
	void municipalityNotFoundException_returns404WithProblemDetail() {
		when(municipalityService.search("nowhere")).thenThrow(new MunicipalityNotFoundException("99999"));

		mockMvcTester.get().uri("/api/municipalities").param("name", "nowhere")
			.assertThat()
			.hasStatus(HttpStatus.NOT_FOUND)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Municipality not found", "detail": "Municipality not found: 99999"}
				""");
	}

	@Test
	void forecastNotAvailableException_returns404WithProblemDetail() {
		when(forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.thenThrow(new ForecastNotAvailableException(MUNICIPALITY_CODE));

		mockMvcTester.get().uri("/api/forecast/" + MUNICIPALITY_CODE)
			.assertThat()
			.hasStatus(HttpStatus.NOT_FOUND)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Forecast not available"}
				""");
	}

	@Test
	void aemetUnavailableException_returns502WithGenericProblemDetail() {
		when(forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.thenThrow(new AemetUnavailableException("boom"));

		mockMvcTester.get().uri("/api/forecast/" + MUNICIPALITY_CODE)
			.assertThat()
			.hasStatus(HttpStatus.BAD_GATEWAY)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Weather provider unavailable", "detail": "Weather data provider is temporarily unavailable"}
				""");
	}

	@Test
	void aemetUnavailableException_doesNotLeakSensitiveInternalDetailInResponseBody() {
		when(forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.thenThrow(new AemetUnavailableException(
				"Failed to parse AEMET JSON response from https://opendata.aemet.es/opendata/sh/secret-url"));

		mockMvcTester.get().uri("/api/forecast/" + MUNICIPALITY_CODE)
			.assertThat()
			.hasStatus(HttpStatus.BAD_GATEWAY)
			.bodyText()
			.doesNotContain("secret-url")
			.doesNotContain("opendata.aemet.es");
	}

	@Test
	void missingNameParameter_returns400WithInvalidRequestProblemDetail() {
		mockMvcTester.get().uri("/api/municipalities")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Invalid request"}
				""");
	}

	@Test
	void blankNameParameter_returns400WithInvalidRequestProblemDetail() {
		mockMvcTester.get().uri("/api/municipalities").param("name", "   ")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Invalid request"}
				""");
	}

	@Test
	void invalidMunicipalityCode_returns400WithInvalidRequestProblemDetail() {
		mockMvcTester.get().uri("/api/forecast/abc")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Invalid request"}
				""");
	}

	@Test
	void invalidTemperatureUnit_returns400WithInvalidRequestProblemDetail() {
		mockMvcTester.get().uri("/api/forecast/" + MUNICIPALITY_CODE).param("temperatureUnit", "INVALID")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.isLenientlyEqualTo("""
				{"title": "Invalid request"}
				""");
	}
}
