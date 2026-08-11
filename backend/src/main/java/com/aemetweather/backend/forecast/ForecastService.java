package com.aemetweather.backend.forecast;

import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.exception.ForecastNotAvailableException;
import com.aemetweather.backend.integration.aemet.AemetClient;
import com.aemetweather.backend.model.DailyForecast;
import com.aemetweather.backend.model.PrecipitationPeriod;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {

	private static final List<String> REQUIRED_PERIODS = List.of("00-06", "06-12", "12-18", "18-24");

	private final AemetClient aemetClient;
	private final Clock clock;

	public ForecastService(AemetClient aemetClient, Clock clock) {
		this.aemetClient = aemetClient;
		this.clock = clock;
	}

	public ForecastResponse getForecast(String municipalityCode, TemperatureUnit temperatureUnit) {
		List<DailyForecast> forecasts = aemetClient.getForecast(municipalityCode);
		DailyForecast tomorrowForecast = findTomorrow(forecasts, municipalityCode);

		double averageCelsius = (tomorrowForecast.maximumTemperature() + tomorrowForecast.minimumTemperature()) / 2.0;
		double convertedTemperature = temperatureUnit == TemperatureUnit.G_FAH ? toFahrenheit(averageCelsius) : averageCelsius;
		double averageTemperature = round(convertedTemperature);

		List<PrecipitationResponse> precipitation = selectRequiredPeriods(tomorrowForecast.precipitationPeriods());

		return new ForecastResponse(averageTemperature, temperatureUnit, precipitation);
	}

	private DailyForecast findTomorrow(List<DailyForecast> forecasts, String municipalityCode) {
		LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
		return forecasts.stream()
			.filter(forecast -> forecast.date().equals(tomorrow))
			.findFirst()
			.orElseThrow(() -> new ForecastNotAvailableException(municipalityCode));
	}

	private double toFahrenheit(double celsius) {
		return celsius * 9.0 / 5.0 + 32.0;
	}

	private double round(double value) {
		return Math.round(value * 10.0) / 10.0;
	}

	private List<PrecipitationResponse> selectRequiredPeriods(List<PrecipitationPeriod> periods) {
		Map<String, Integer> probabilityByPeriod = periods.stream()
			.collect(Collectors.toMap(PrecipitationPeriod::period, PrecipitationPeriod::probability, (first, second) -> first));

		return REQUIRED_PERIODS.stream()
			.map(period -> {
				Integer probability = probabilityByPeriod.get(period);
				if (probability == null) {
					throw new AemetUnavailableException("AEMET forecast is missing required precipitation period: " + period);
				}
				return new PrecipitationResponse(probability, period);
			})
			.toList();
	}
}
