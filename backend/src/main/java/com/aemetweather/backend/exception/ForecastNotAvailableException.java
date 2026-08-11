package com.aemetweather.backend.exception;

public class ForecastNotAvailableException extends RuntimeException {

	public ForecastNotAvailableException(String municipalityCode) {
		super("Forecast not available for tomorrow: " + municipalityCode);
	}
}
