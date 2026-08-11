package com.aemetweather.backend.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ForecastController {

	private final ForecastService forecastService;

	public ForecastController(ForecastService forecastService) {
		this.forecastService = forecastService;
	}

	@GetMapping("/api/forecast/{municipalityCode}")
	public ForecastResponse getForecast(
			@PathVariable @NotBlank @Pattern(regexp = "\\d{5}") String municipalityCode,
			@RequestParam(defaultValue = "G_CEL") TemperatureUnit temperatureUnit) {
		return forecastService.getForecast(municipalityCode, temperatureUnit);
	}
}
