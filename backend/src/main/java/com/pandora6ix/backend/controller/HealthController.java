package com.pandora6ix.backend.controller;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Basic liveness endpoint; no business state or database connection is required. */
@RestController
public class HealthController {
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> health() { return Map.of("status", "UP"); }
}
