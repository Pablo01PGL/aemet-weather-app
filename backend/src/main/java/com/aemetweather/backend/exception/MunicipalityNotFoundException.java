package com.aemetweather.backend.exception;

public class MunicipalityNotFoundException extends RuntimeException {

	public MunicipalityNotFoundException(String municipalityCode) {
		super("Municipality not found: " + municipalityCode);
	}
}
