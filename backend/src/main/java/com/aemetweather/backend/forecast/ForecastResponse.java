package com.aemetweather.backend.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ForecastResponse(
	@JsonProperty("mediaTemperatura") double averageTemperature,
	@JsonProperty("unidadTemperatura") TemperatureUnit temperatureUnit,
	@JsonProperty("probPrecipitacion") List<PrecipitationResponse> precipitation) {
}
