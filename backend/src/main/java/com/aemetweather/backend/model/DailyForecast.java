package com.aemetweather.backend.model;

import java.time.LocalDate;
import java.util.List;

public record DailyForecast(
	LocalDate date,
	int maximumTemperature,
	int minimumTemperature,
	List<PrecipitationPeriod> precipitationPeriods) {
}
