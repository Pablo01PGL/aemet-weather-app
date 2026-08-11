package com.aemetweather.backend.integration.aemet;

import com.aemetweather.backend.config.AemetProperties;
import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.integration.aemet.dto.AemetEnvelope;
import com.aemetweather.backend.integration.aemet.dto.AemetMunicipalityDto;
import com.aemetweather.backend.model.Municipality;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AemetRestClient implements AemetClient {

	private static final String API_KEY_HEADER = "api_key";
	private static final String MUNICIPALITIES_PATH = "/maestro/municipios";
	private static final String AEMET_ID_PREFIX = "id";

	private final RestClient restClient;
	private final AemetProperties properties;

	public AemetRestClient(RestClient.Builder restClientBuilder, AemetProperties properties) {
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
		this.properties = properties;
	}

	@Override
	public List<Municipality> getMunicipalities() {
		String dataUrl = fetchDataUrl();
		List<AemetMunicipalityDto> municipalities = fetchMunicipalities(dataUrl);
		return municipalities.stream().map(this::toMunicipality).toList();
	}

	private String fetchDataUrl() {
		AemetEnvelope envelope;
		try {
			envelope = restClient.get()
				.uri(MUNICIPALITIES_PATH)
				.header(API_KEY_HEADER, properties.apiKey())
				.retrieve()
				.body(AemetEnvelope.class);
		}
		catch (RestClientException e) {
			throw new AemetUnavailableException("Failed to retrieve municipalities envelope from AEMET", e);
		}

		if (envelope == null || envelope.dataUrl() == null || envelope.dataUrl().isBlank()) {
			throw new AemetUnavailableException("AEMET envelope did not contain a valid data URL");
		}
		return envelope.dataUrl();
	}

	private List<AemetMunicipalityDto> fetchMunicipalities(String dataUrl) {
		try {
			List<AemetMunicipalityDto> municipalities = restClient.get()
				.uri(dataUrl)
				.retrieve()
				.body(new ParameterizedTypeReference<List<AemetMunicipalityDto>>() {
				});
			if (municipalities == null) {
				throw new AemetUnavailableException("AEMET returned an empty municipalities payload");
			}
			return municipalities;
		}
		catch (RestClientException e) {
			throw new AemetUnavailableException("Failed to retrieve municipalities data from AEMET", e);
		}
	}

	private Municipality toMunicipality(AemetMunicipalityDto dto) {
		String code = dto.id().startsWith(AEMET_ID_PREFIX) ? dto.id().substring(AEMET_ID_PREFIX.length()) : dto.id();
		return new Municipality(code, dto.name());
	}
}
