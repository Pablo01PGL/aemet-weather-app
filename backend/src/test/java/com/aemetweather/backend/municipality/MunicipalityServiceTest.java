package com.aemetweather.backend.municipality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aemetweather.backend.integration.aemet.AemetClient;
import com.aemetweather.backend.model.Municipality;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MunicipalityServiceTest {

	@Mock
	private AemetClient aemetClient;

	private MunicipalityService municipalityService;

	@BeforeEach
	void setUp() {
		municipalityService = new MunicipalityService(aemetClient);
	}

	@Test
	void search_matchesPartialName() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("03014", "Alicante/Alacant"),
			new Municipality("28079", "Madrid")));

		List<MunicipalityResponse> result = municipalityService.search("alican");

		assertThat(result).containsExactly(new MunicipalityResponse("03014", "Alicante/Alacant"));
	}

	@Test
	void search_isCaseInsensitive() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("03014", "Alicante/Alacant")));

		List<MunicipalityResponse> result = municipalityService.search("ALICAN");

		assertThat(result).containsExactly(new MunicipalityResponse("03014", "Alicante/Alacant"));
	}

	@Test
	void search_isAccentInsensitive_queryWithoutAccent() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("10001", "Abadía")));

		List<MunicipalityResponse> result = municipalityService.search("abadia");

		assertThat(result).containsExactly(new MunicipalityResponse("10001", "Abadía"));
	}

	@Test
	void search_isAccentInsensitive_queryWithAccent() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("10001", "Abadia")));

		List<MunicipalityResponse> result = municipalityService.search("abadía");

		assertThat(result).containsExactly(new MunicipalityResponse("10001", "Abadia"));
	}

	@Test
	void search_withNoMatches_returnsEmptyList() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("28079", "Madrid")));

		List<MunicipalityResponse> result = municipalityService.search("zzz");

		assertThat(result).isEmpty();
	}

	@Test
	void search_returnsMultipleMatches() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("44001", "Ababuj"),
			new Municipality("10001", "Abadía"),
			new Municipality("28079", "Madrid")));

		List<MunicipalityResponse> result = municipalityService.search("aba");

		assertThat(result).containsExactly(
			new MunicipalityResponse("44001", "Ababuj"),
			new MunicipalityResponse("10001", "Abadía"));
	}

	@Test
	void search_mapsMunicipalityToMunicipalityResponse() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("44001", "Ababuj")));

		List<MunicipalityResponse> result = municipalityService.search("ababuj");

		assertThat(result).containsExactly(new MunicipalityResponse("44001", "Ababuj"));
	}

	@Test
	void search_doesNotDeduplicateMunicipalitiesWithSameName() {
		when(aemetClient.getMunicipalities()).thenReturn(List.of(
			new Municipality("10001", "San Martín"),
			new Municipality("20002", "San Martín")));

		List<MunicipalityResponse> result = municipalityService.search("san martin");

		assertThat(result).containsExactly(
			new MunicipalityResponse("10001", "San Martín"),
			new MunicipalityResponse("20002", "San Martín"));
	}
}
