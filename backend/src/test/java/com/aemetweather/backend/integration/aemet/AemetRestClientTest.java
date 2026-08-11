package com.aemetweather.backend.integration.aemet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.aemetweather.backend.config.AemetProperties;
import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.exception.MunicipalityNotFoundException;
import com.aemetweather.backend.model.DailyForecast;
import com.aemetweather.backend.model.Municipality;
import com.aemetweather.backend.model.PrecipitationPeriod;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class AemetRestClientTest {

	private static final String BASE_URL = "https://opendata.aemet.es/opendata/api";
	private static final String API_KEY = "test-api-key";
	private static final String DATA_URL = "https://opendata.aemet.es/data/municipios/abc123";
	private static final String MUNICIPALITY_CODE = "44001";
	private static final String FORECAST_DATA_URL = "https://opendata.aemet.es/data/prediccion/abc456";
	private static final Charset ISO_8859_15 = Charset.forName("ISO-8859-15");
	private static final MediaType AEMET_DATA_CONTENT_TYPE = new MediaType("text", "plain", ISO_8859_15);

	private MockRestServiceServer server;
	private AemetRestClient aemetRestClient;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		AemetProperties properties = new AemetProperties(BASE_URL, API_KEY);
		aemetRestClient = new AemetRestClient(builder, properties, JsonMapper.builder().build());
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
	void getMunicipalities_whenSecondResponseDeclaresIso88595AndBodyIsIso88595Encoded_preservesAccents() {
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));

		String json = """
			[
			  {
			    "id": "id10001",
			    "nombre": "Abadía"
			  },
			  {
			    "id": "id48001",
			    "nombre": "Abadiño"
			  }
			]
			""";
		server.expect(requestTo(DATA_URL))
			.andRespond(withStatus(HttpStatus.OK)
				.body(json.getBytes(ISO_8859_15))
				.contentType(AEMET_DATA_CONTENT_TYPE));

		List<Municipality> result = aemetRestClient.getMunicipalities();

		assertThat(result).containsExactly(
			new Municipality("10001", "Abadía"),
			new Municipality("48001", "Abadiño"));
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

	@Test
	void getForecast_sendsApiKeyOnlyOnFirstRequestAndMapsAllDaysAndPeriods() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("api_key", API_KEY))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andExpect(method(HttpMethod.GET))
			.andExpect(request -> assertThat(request.getHeaders().containsHeader("api_key")).isFalse())
			.andRespond(withSuccess(forecastJson(), MediaType.APPLICATION_JSON));

		List<DailyForecast> result = aemetRestClient.getForecast(MUNICIPALITY_CODE);

		List<PrecipitationPeriod> expectedFirstDayPeriods = List.of(
			new PrecipitationPeriod(5, "00-24"),
			new PrecipitationPeriod(0, "00-12"),
			new PrecipitationPeriod(5, "12-24"),
			new PrecipitationPeriod(0, "00-06"),
			new PrecipitationPeriod(0, "06-12"),
			new PrecipitationPeriod(5, "12-18"),
			new PrecipitationPeriod(0, "18-24"));
		assertThat(result).containsExactly(
			new DailyForecast(LocalDate.of(2026, 8, 12), 34, 17, expectedFirstDayPeriods),
			new DailyForecast(LocalDate.of(2026, 8, 13), 30, 15, List.of(new PrecipitationPeriod(10, "00-24"))));
		server.verify();
	}

	@Test
	void getForecast_whenSecondResponseDeclaresTextPlainIso88595Charset_stillParsesJson() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andRespond(withStatus(HttpStatus.OK)
				.body(forecastJson().getBytes(ISO_8859_15))
				.contentType(AEMET_DATA_CONTENT_TYPE));

		List<DailyForecast> result = aemetRestClient.getForecast(MUNICIPALITY_CODE);

		assertThat(result).hasSize(2);
		server.verify();
	}

	@Test
	void getForecast_whenFirstRequestReturnsNotFound_throwsMunicipalityNotFoundException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(MunicipalityNotFoundException.class);
	}

	@Test
	void getForecast_whenFirstRequestReturnsServerError_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withServerError());

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getForecast_whenSecondRequestReturnsNotFound_throwsAemetUnavailableExceptionNotMunicipalityNotFound() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(AemetUnavailableException.class)
			.isNotInstanceOf(MunicipalityNotFoundException.class);
	}

	@Test
	void getForecast_whenSecondRequestReturnsServerError_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andRespond(withServerError());

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getForecast_whenDataJsonIsInvalid_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getForecast_whenEnvelopeHasNoDataUrl_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess("""
				{"descripcion":"exito","estado":200}""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
			.isInstanceOf(AemetUnavailableException.class);
	}

	@Test
	void getForecast_whenForecastListIsEmpty_throwsAemetUnavailableException() {
		server.expect(requestTo(BASE_URL + "/prediccion/especifica/municipio/diaria/" + MUNICIPALITY_CODE))
			.andRespond(withSuccess(forecastEnvelopeJson(), MediaType.APPLICATION_JSON));

		server.expect(requestTo(FORECAST_DATA_URL))
			.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> aemetRestClient.getForecast(MUNICIPALITY_CODE))
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

	private String forecastEnvelopeJson() {
		return """
			{
			  "descripcion": "exito",
			  "estado": 200,
			  "datos": "%s",
			  "metadatos": "https://opendata.aemet.es/data/metadatos/abc456"
			}
			""".formatted(FORECAST_DATA_URL);
	}

	private String forecastJson() {
		return """
			[
			  {
			    "origen": {
			      "productor": "AEMET"
			    },
			    "prediccion": {
			      "dia": [
			        {
			          "fecha": "2026-08-12T00:00:00",
			          "probPrecipitacion": [
			            {"value": 5, "periodo": "00-24"},
			            {"value": 0, "periodo": "00-12"},
			            {"value": 5, "periodo": "12-24"},
			            {"value": 0, "periodo": "00-06"},
			            {"value": 0, "periodo": "06-12"},
			            {"value": 5, "periodo": "12-18"},
			            {"value": 0, "periodo": "18-24"}
			          ],
			          "temperatura": {
			            "maxima": 34,
			            "minima": 17,
			            "dato": [
			              {"value": 18, "hora": 6}
			            ]
			          },
			          "estadoCielo": [],
			          "viento": [],
			          "sensTermica": {},
			          "humedadRelativa": {},
			          "uvMax": 9
			        },
			        {
			          "fecha": "2026-08-13T00:00:00",
			          "probPrecipitacion": [
			            {"value": 10, "periodo": "00-24"}
			          ],
			          "temperatura": {
			            "maxima": 30,
			            "minima": 15
			          }
			        }
			      ]
			    }
			  }
			]
			""";
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
