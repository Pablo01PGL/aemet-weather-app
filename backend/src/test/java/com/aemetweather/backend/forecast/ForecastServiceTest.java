package com.aemetweather.backend.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.exception.ForecastNotAvailableException;
import com.aemetweather.backend.exception.MunicipalityNotFoundException;
import com.aemetweather.backend.integration.aemet.AemetClient;
import com.aemetweather.backend.model.DailyForecast;
import com.aemetweather.backend.model.PrecipitationPeriod;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

	private static final String MUNICIPALITY_CODE = "44001";
	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

	@Mock
	private AemetClient aemetClient;

	private ForecastService forecastService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(LocalDate.of(2026, 8, 11).atStartOfDay(MADRID).toInstant(), MADRID);
		forecastService = new ForecastService(aemetClient, clock);
	}

	@Test
	void getForecast_selectsTomorrowByDateRegardlessOfListOrder() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 13), 30, 15, allPeriods()),
			dailyForecast(LocalDate.of(2026, 8, 11), 20, 10, allPeriods()),
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL);

		assertThat(response.averageTemperature()).isEqualTo(25.5);
	}

	@Test
	void getForecast_calculatesCelsiusAverage() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL);

		assertThat(response.averageTemperature()).isEqualTo(25.5);
	}

	@Test
	void getForecast_convertsToFahrenheit() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_FAH);

		assertThat(response.averageTemperature()).isEqualTo(77.9);
	}

	@Test
	void getForecast_roundsToOneDecimalPlace() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 30, 19, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_FAH);

		assertThat(response.averageTemperature()).isEqualTo(76.1);
	}

	@Test
	void getForecast_celsiusAppliesNoConversion() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL);

		assertThat(response.temperatureUnit()).isEqualTo(TemperatureUnit.G_CEL);
		assertThat(response.averageTemperature()).isEqualTo(25.5);
	}

	@Test
	void getForecast_filtersToTheFourRequiredPeriods() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, allPeriods())));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL);

		assertThat(response.precipitation()).extracting(PrecipitationResponse::period)
			.containsExactly("00-06", "06-12", "12-18", "18-24");
	}

	@Test
	void getForecast_precipitationOutputIsAlwaysInFixedOrder_evenWhenAemetIsUnordered() {
		List<PrecipitationPeriod> shuffled = List.of(
			new PrecipitationPeriod(5, "18-24"),
			new PrecipitationPeriod(0, "00-24"),
			new PrecipitationPeriod(0, "12-18"),
			new PrecipitationPeriod(0, "00-12"),
			new PrecipitationPeriod(0, "06-12"),
			new PrecipitationPeriod(5, "12-24"),
			new PrecipitationPeriod(0, "00-06"));
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, shuffled)));

		ForecastResponse response = forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL);

		assertThat(response.precipitation()).extracting(PrecipitationResponse::period)
			.containsExactly("00-06", "06-12", "12-18", "18-24");
		assertThat(response.precipitation()).extracting(PrecipitationResponse::probability)
			.containsExactly(0, 0, 0, 5);
	}

	@Test
	void getForecast_whenTomorrowIsMissing_throwsForecastNotAvailableException() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 13), 30, 15, allPeriods())));

		assertThatThrownBy(() -> forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.isInstanceOf(ForecastNotAvailableException.class);
	}

	@Test
	void getForecast_doesNotTranslateMunicipalityNotFoundException() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenThrow(new MunicipalityNotFoundException(MUNICIPALITY_CODE));

		assertThatThrownBy(() -> forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.isInstanceOf(MunicipalityNotFoundException.class);
	}

	@Test
	void getForecast_doesNotTranslateAemetUnavailableException() {
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenThrow(new AemetUnavailableException("boom"));

		assertThatThrownBy(() -> forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getForecast_whenTomorrowExistsButIsMissingARequiredPrecipitationPeriod_throwsAemetUnavailableExceptionWithoutInventingProbability() {
		List<PrecipitationPeriod> missingOneRequiredPeriod = List.of(
			new PrecipitationPeriod(5, "00-24"),
			new PrecipitationPeriod(0, "00-06"),
			new PrecipitationPeriod(0, "06-12"),
			new PrecipitationPeriod(5, "12-18"));
		when(aemetClient.getForecast(MUNICIPALITY_CODE)).thenReturn(List.of(
			dailyForecast(LocalDate.of(2026, 8, 12), 34, 17, missingOneRequiredPeriod)));

		assertThatThrownBy(() -> forecastService.getForecast(MUNICIPALITY_CODE, TemperatureUnit.G_CEL))
			.isInstanceOf(AemetUnavailableException.class)
			.isNotInstanceOf(ForecastNotAvailableException.class);
	}

	private DailyForecast dailyForecast(LocalDate date, int maximumTemperature, int minimumTemperature, List<PrecipitationPeriod> periods) {
		return new DailyForecast(date, maximumTemperature, minimumTemperature, periods);
	}

	private List<PrecipitationPeriod> allPeriods() {
		return List.of(
			new PrecipitationPeriod(5, "00-24"),
			new PrecipitationPeriod(0, "00-12"),
			new PrecipitationPeriod(5, "12-24"),
			new PrecipitationPeriod(0, "00-06"),
			new PrecipitationPeriod(0, "06-12"),
			new PrecipitationPeriod(5, "12-18"),
			new PrecipitationPeriod(0, "18-24"));
	}
}
