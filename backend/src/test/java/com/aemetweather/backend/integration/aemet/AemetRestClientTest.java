package com.aemetweather.backend.integration.aemet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.aemetweather.backend.config.AemetProperties;
import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.model.Municipality;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AemetRestClientTest {

	private static final String BASE_URL = "https://opendata.aemet.es/opendata/api";
	private static final String API_KEY = "test-api-key";
	private static final String DATA_URL = "https://opendata.aemet.es/data/municipios/abc123";

	private MockRestServiceServer server;
	private AemetRestClient aemetRestClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		AemetProperties properties = new AemetProperties(BASE_URL, API_KEY);
		aemetRestClient = new AemetRestClient(builder, properties);
	}

	@Test
	void getMunicipalities_sendsApiKeyOnlyOnFirstRequestAndMapsResult() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("api_key", API_KEY))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(DATA_URL))
			.andExpect(method(HttpMethod.GET))
			.andExpect(request -> assertThat(request.getHeaders().containsHeader("api_key")).isFalse())
			.andRespond(withSuccess(municipalitiesJson(), MediaType.APPLICATION_JSON));

		List<Municipality> result = aemetRestClient.getMunicipalities();

		assertThat(result).containsExactly(
			new Municipality("44001", "Ababuj"),
			new Municipality("28079", "Madrid"));
		server.verify();
	}

	@Test
	void getMunicipalities_whenFirstRequestReturnsServerError_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withServerError());

		assertThatThrownBy(() -> aemetRestClient.getMunicipalities())
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getMunicipalities_whenEnvelopeIsInvalidJson_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> aemetRestClient.getMunicipalities())
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getMunicipalities_whenEnvelopeHasNoDataUrl_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess("""
				{"descripcion":"exito","estado":200}""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> aemetRestClient.getMunicipalities())
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getMunicipalities_whenSecondRequestFails_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(DATA_URL))
			.andRespond(withServerError());

		assertThatThrownBy(() -> aemetRestClient.getMunicipalities())
			.isInstanceOf(AemetUnavailableException.class);
	}

	private String envelopeJson() {
		return """
			{
			  "descripcion": "exito",
			  "estado": 200,
			  "datos": "%s",
			  "metadatos": "https://opendata.aemet.es/data/metadatos/abc123"
			}
			""".formatted(DATA_URL);
	}

	private String municipalitiesJson() {
		return """
			[
			  {
			    "latitud": "40.76",
			    "id_old": "44004",
			    "url": "ababuj-id44001",
			    "capital": "Ababuj",
			    "nombre": "Ababuj",
			    "id": "id44001"
			  },
			  {
			    "latitud": "40.42",
			    "id_old": "28079",
			    "url": "madrid-id28079",
			    "capital": "Madrid",
			    "nombre": "Madrid",
			    "id": "id28079"
			  }
			]
			""";
	}
}
