package com.aemetweather.backend.municipality;

import com.aemetweather.backend.integration.aemet.AemetClient;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MunicipalityService {

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	private final AemetClient aemetClient;

	public MunicipalityService(AemetClient aemetClient) {
		this.aemetClient = aemetClient;
	}

	public List<MunicipalityResponse> search(String name) {
		String normalizedQuery = normalize(name);
		return aemetClient.getMunicipalities().stream()
			.filter(municipality -> normalize(municipality.name()).contains(normalizedQuery))
			.map(municipality -> new MunicipalityResponse(municipality.code(), municipality.name()))
			.toList();
	}

	private String normalize(String value) {
		String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
		return withoutDiacritics.toLowerCase(Locale.ROOT);
	}
}
