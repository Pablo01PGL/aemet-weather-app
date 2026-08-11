package com.aemetweather.backend.integration.aemet;

import com.aemetweather.backend.config.AemetProperties;
import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.exception.MunicipalityNotFoundException;
import com.aemetweather.backend.integration.aemet.dto.AemetDayDto;
import com.aemetweather.backend.integration.aemet.dto.AemetEnvelope;
import com.aemetweather.backend.integration.aemet.dto.AemetForecastMunicipalityDto;
import com.aemetweather.backend.integration.aemet.dto.AemetMunicipalityDto;
import com.aemetweather.backend.integration.aemet.dto.AemetPrecipitationProbabilityDto;
import com.aemetweather.backend.model.DailyForecast;
import com.aemetweather.backend.model.Municipality;
import com.aemetweather.backend.model.PrecipitationPeriod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class AemetRestClient implements AemetClient {

	private static final String API_KEY_HEADER = "api_key";
	private static final String MUNICIPALITIES_PATH = "/maestro/municipios";
	private static final String FORECAST_PATH = "/prediccion/especifica/municipio/diaria/{municipalityCode}";
	private static final String AEMET_ID_PREFIX = "id";

	private final RestClient restClient;
	private final AemetProperties properties;
	private final ObjectMapper objectMapper;

	public AemetRestClient(RestClient.Builder restClientBuilder, AemetProperties properties, ObjectMapper objectMapper) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public List<Municipality> getMunicipalities() {
		String dataUrl = fetchDataUrl(MUNICIPALITIES_PATH);
		List<AemetMunicipalityDto> municipalities = fetchMunicipalities(dataUrl);
		return municipalities.stream().map(this::toMunicipality).toList();
	}

	@Override
	public List<DailyForecast> getForecast(String municipalityCode) {
		String dataUrl = fetchForecastDataUrl(municipalityCode);
		List<AemetForecastMunicipalityDto> forecasts = fetchForecast(dataUrl);
		return toDailyForecasts(forecasts);
	}

	private String fetchDataUrl(String path) {
		AemetEnvelope envelope;
		try {
			envelope = restClient.get()
				.uri(path)
				.header(API_KEY_HEADER, properties.apiKey())
				.retrieve()
				.body(AemetEnvelope.class);
		}
		catch (RestClientException e) {
			throw new AemetUnavailableException("Failed to retrieve envelope from AEMET", e);
		}

		if (envelope == null || envelope.dataUrl() == null || envelope.dataUrl().isBlank()) {
			throw new AemetUnavailableException("AEMET envelope did not contain a valid data URL");
		}
		return envelope.dataUrl();
	}

	private String fetchForecastDataUrl(String municipalityCode) {
		AemetEnvelope envelope;
		try {
			envelope = restClient.get()
				.uri(FORECAST_PATH, municipalityCode)
				.header(API_KEY_HEADER, properties.apiKey())
				.retrieve()
				.body(AemetEnvelope.class);
		}
		catch (HttpClientErrorException.NotFound e) {
			throw new MunicipalityNotFoundException(municipalityCode);
		}
		catch (RestClientException e) {
			throw new AemetUnavailableException("Failed to retrieve forecast envelope from AEMET", e);
		}

		if (envelope == null || envelope.dataUrl() == null || envelope.dataUrl().isBlank()) {
			throw new MunicipalityNotFoundException(municipalityCode);
		}
		return envelope.dataUrl();
	}

	private List<AemetMunicipalityDto> fetchMunicipalities(String dataUrl) {
		String json = fetchRawJson(dataUrl);
		List<AemetMunicipalityDto> municipalities = readJson(json, new TypeReference<List<AemetMunicipalityDto>>() {
		});
		if (municipalities == null) {
			throw new AemetUnavailableException("AEMET returned an empty municipalities payload");
		}
		return municipalities;
	}

	private List<AemetForecastMunicipalityDto> fetchForecast(String dataUrl) {
		String json = fetchRawJson(dataUrl);
		List<AemetForecastMunicipalityDto> forecasts = readJson(json, new TypeReference<List<AemetForecastMunicipalityDto>>() {
		});
		if (forecasts == null || forecasts.isEmpty()) {
			throw new AemetUnavailableException("AEMET returned an empty forecast payload");
		}
		return forecasts;
	}

	private String fetchRawJson(String dataUrl) {
		String body;
		try {
			body = restClient.get().uri(dataUrl).retrieve().body(String.class);
		}
		catch (RestClientException e) {
			throw new AemetUnavailableException("Failed to retrieve data from AEMET", e);
		}

		if (body == null) {
			throw new AemetUnavailableException("AEMET returned an empty response body");
		}
		return body;
	}

	private <T> T readJson(String json, TypeReference<T> typeReference) {
		try {
			return objectMapper.readValue(json, typeReference);
		}
		catch (JacksonException e) {
			throw new AemetUnavailableException("Failed to parse AEMET JSON response", e);
		}
	}

	private Municipality toMunicipality(AemetMunicipalityDto dto) {
		String code = dto.id().startsWith(AEMET_ID_PREFIX) ? dto.id().substring(AEMET_ID_PREFIX.length()) : dto.id();
		return new Municipality(code, dto.name());
	}

	private List<DailyForecast> toDailyForecasts(List<AemetForecastMunicipalityDto> forecasts) {
		AemetForecastMunicipalityDto forecastMunicipality = forecasts.get(0);
		if (forecastMunicipality == null || forecastMunicipality.forecast() == null
				|| forecastMunicipality.forecast().days() == null) {
			throw new AemetUnavailableException("AEMET forecast response did not contain any days");
		}
		return forecastMunicipality.forecast().days().stream().map(this::toDailyForecast).toList();
	}

	private DailyForecast toDailyForecast(AemetDayDto day) {
		if (day.temperature() == null || day.temperature().maximum() == null || day.temperature().minimum() == null) {
			throw new AemetUnavailableException("AEMET forecast day is missing required temperature data");
		}
		LocalDate date = parseDate(day.date());
		List<PrecipitationPeriod> precipitationPeriods = day.precipitationProbabilities() == null
			? List.of()
			: day.precipitationProbabilities().stream().map(this::toPrecipitationPeriod).toList();
		return new DailyForecast(date, day.temperature().maximum(), day.temperature().minimum(), precipitationPeriods);
	}

	private PrecipitationPeriod toPrecipitationPeriod(AemetPrecipitationProbabilityDto dto) {
		int probability = dto.probability() == null ? 0 : dto.probability();
		return new PrecipitationPeriod(probability, dto.period());
	}

	private LocalDate parseDate(String date) {
		if (date == null || date.isBlank()) {
			throw new AemetUnavailableException("AEMET forecast day is missing a date");
		}
		try {
			return LocalDateTime.parse(date).toLocalDate();
		}
		catch (DateTimeParseException e) {
			throw new AemetUnavailableException("AEMET forecast day has an invalid date: " + date, e);
		}
	}
}
