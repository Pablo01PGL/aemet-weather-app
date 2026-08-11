package com.aemetweather.backend.integration.aemet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.aemetweather.backend.config.AemetProperties;
import com.aemetweather.backend.exception.AemetUnavailableException;
import com.aemetweather.backend.model.Municipality;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// Uses a real ApplicationContext (not "new AemetRestClient(...)") so the @Cacheable proxy is genuinely exercised.
class AemetRestClientCachingTest {

	private static final String BASE_URL = "https://opendata.aemet.es/opendata/api";
	private static final String API_KEY = "test-api-key";
	private static final String DATA_URL = "https://opendata.aemet.es/data/municipios/abc123";

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void closeContext() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void getMunicipalities_secondAndThirdCallsReuseTheCachedCatalogInsteadOfCallingAemetAgain() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));
		server.expect(requestTo(DATA_URL))
			.andRespond(withSuccess(municipalitiesJson(), MediaType.APPLICATION_JSON));

		AemetClient aemetClient = createClient(builder);

		List<Municipality> first = aemetClient.getMunicipalities();
		List<Municipality> second = aemetClient.getMunicipalities();
		List<Municipality> third = aemetClient.getMunicipalities();

		// Only one envelope+data pair is stubbed; a repeated AEMET call would fail server.verify().
		assertThat(first).containsExactly(new Municipality("44001", "Ababuj"));
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
		server.verify();
	}

	@Test
	void getMunicipalities_searchesForDifferentTermsShareTheSameCachedCatalog() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));
		server.expect(requestTo(DATA_URL))
			.andRespond(withSuccess(municipalitiesJson(), MediaType.APPLICATION_JSON));

		AemetClient aemetClient = createClient(builder);

		List<Municipality> forMadridSearch = aemetClient.getMunicipalities();
		List<Municipality> forSevillaSearch = aemetClient.getMunicipalities();

		assertThat(forSevillaSearch).isEqualTo(forMadridSearch);
		server.verify();
	}

	@Test
	void getMunicipalities_whenFirstLoadFails_doesNotCacheTheErrorAndRetriesOnNextCall() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(BASE_URL + "/maestro/municipios")).andRespond(withServerError());

		AemetClient aemetClient = createClient(builder);

		assertThatThrownBy(aemetClient::getMunicipalities).isInstanceOf(AemetUnavailableException.class);

		server.reset();
		server.expect(requestTo(BASE_URL + "/maestro/municipios"))
			.andRespond(withSuccess(envelopeJson(), MediaType.APPLICATION_JSON));
		server.expect(requestTo(DATA_URL))
			.andRespond(withSuccess(municipalitiesJson(), MediaType.APPLICATION_JSON));

		List<Municipality> result = aemetClient.getMunicipalities();

		assertThat(result).containsExactly(new Municipality("44001", "Ababuj"));
		server.verify();
	}

	private AemetClient createClient(RestClient.Builder builder) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(CachingTestConfig.class);
		ctx.registerBean(RestClient.Builder.class, () -> builder);
		ctx.registerBean(AemetProperties.class, () -> new AemetProperties(BASE_URL, API_KEY));
		ctx.registerBean(ObjectMapper.class, () -> JsonMapper.builder().build());
		ctx.registerBean(AemetRestClient.class);
		ctx.refresh();
		this.context = ctx;
		return ctx.getBean(AemetClient.class);
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
			    "nombre": "Ababuj",
			    "id": "id44001"
			  }
			]
			""";
	}

	@Configuration
	@EnableCaching
	static class CachingTestConfig {

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}
	}
}
