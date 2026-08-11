package com.aemetweather.backend.municipality;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MunicipalityController {

	private final MunicipalityService municipalityService;

	public MunicipalityController(MunicipalityService municipalityService) {
		this.municipalityService = municipalityService;
	}

	@GetMapping("/api/municipalities")
	public List<MunicipalityResponse> search(@RequestParam @NotBlank String name) {
		return municipalityService.search(name);
	}
}
