package com.aemetweather.backend.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PrecipitationResponse(
	@JsonProperty("probabilidad") int probability,
	@JsonProperty("periodo") String period) {
}
