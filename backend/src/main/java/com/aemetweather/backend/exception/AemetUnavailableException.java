package com.aemetweather.backend.exception;

public class AemetUnavailableException extends RuntimeException {

	public AemetUnavailableException(String message) {
		super(message);
	}

	public AemetUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
