package com.aemetweather.backend.integration.aemet;

import com.aemetweather.backend.model.DailyForecast;
import com.aemetweather.backend.model.Municipality;
import java.util.List;

public interface AemetClient {

	List<Municipality> getMunicipalities();

	List<DailyForecast> getForecast(String municipalityCode);
}
