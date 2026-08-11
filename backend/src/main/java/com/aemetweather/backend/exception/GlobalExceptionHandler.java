package com.aemetweather.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MunicipalityNotFoundException.class)
	public ProblemDetail handleMunicipalityNotFound(MunicipalityNotFoundException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
		problem.setTitle("Municipality not found");
		return problem;
	}

	@ExceptionHandler(ForecastNotAvailableException.class)
	public ProblemDetail handleForecastNotAvailable(ForecastNotAvailableException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
		problem.setTitle("Forecast not available");
		return problem;
	}

	@ExceptionHandler(AemetUnavailableException.class)
	public ProblemDetail handleAemetUnavailable(AemetUnavailableException e) {
		log.warn("AEMET provider failure", e);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_GATEWAY, "Weather data provider is temporarily unavailable");
		problem.setTitle("Weather provider unavailable");
		return problem;
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(MissingServletRequestParameterException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST, "Required parameter '" + e.getParameterName() + "' is missing");
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + e.getName() + "'");
		problem.setTitle("Invalid request");
		return problem;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleMethodValidation(HandlerMethodValidationException e) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
		problem.setTitle("Invalid request");
		return problem;
	}
}
