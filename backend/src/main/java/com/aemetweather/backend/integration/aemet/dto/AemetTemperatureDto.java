package com.aemetweather.backend.integration.aemet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AemetTemperatureDto(
	@JsonProperty("maxima") Integer maximum,
	@JsonProperty("minima") Integer minimum) {
}
