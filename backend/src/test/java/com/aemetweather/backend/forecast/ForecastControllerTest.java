package com.aemetweather.backend.forecast;

import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ForecastController.class)
class ForecastControllerTest {

	@Autowired
	private MockMvcTester mockMvcTester;

	@MockitoBean
	private ForecastService forecastService;

	@Test
	void getForecast_withoutTemperatureUnit_defaultsToCelsiusAndReturnsExpectedJson() {
		ForecastResponse response = new ForecastResponse(25.5, TemperatureUnit.G_CEL, List.of(
			new PrecipitationResponse(0, "00-06"),
			new PrecipitationResponse(0, "06-12"),
			new PrecipitationResponse(5, "12-18"),
			new PrecipitationResponse(0, "18-24")));
		when(forecastService.getForecast("44001", TemperatureUnit.G_CEL)).thenReturn(response);

		mockMvcTester.get().uri("/api/forecast/44001")
			.assertThat()
			.hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
				{
				  "mediaTemperatura": 25.5,
				  "unidadTemperatura": "G_CEL",
				  "probPrecipitacion": [
				    {"probabilidad": 0, "periodo": "00-06"},
				    {"probabilidad": 0, "periodo": "06-12"},
				    {"probabilidad": 5, "periodo": "12-18"},
				    {"probabilidad": 0, "periodo": "18-24"}
				  ]
				}
				""");
	}

	@Test
	void getForecast_withFahrenheitParam_passesUnitToServiceAndReturns200() {
		ForecastResponse response = new ForecastResponse(77.9, TemperatureUnit.G_FAH, List.of());
		when(forecastService.getForecast("44001", TemperatureUnit.G_FAH)).thenReturn(response);

		mockMvcTester.get().uri("/api/forecast/44001").param("temperatureUnit", "G_FAH")
			.assertThat()
			.hasStatusOk();
	}

	@Test
	void getForecast_withInvalidTemperatureUnit_returns400() {
		mockMvcTester.get().uri("/api/forecast/44001").param("temperatureUnit", "INVALID")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void getForecast_withInvalidMunicipalityCode_returns400() {
		mockMvcTester.get().uri("/api/forecast/abc")
			.assertThat()
			.hasStatus(HttpStatus.BAD_REQUEST);
	}
}
