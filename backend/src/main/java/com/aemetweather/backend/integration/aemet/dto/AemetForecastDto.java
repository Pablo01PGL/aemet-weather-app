package com.aemetweather.backend.integration.aemet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AemetForecastDto(@JsonProperty("dia") List<AemetDayDto> days) {
}
