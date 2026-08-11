package com.aemetweather.backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aemet")
public record AemetProperties(@NotBlank String baseUrl, @NotBlank String apiKey) {
}
