package com.devpath.platform.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalHealthController {
    private final String status;

    InternalHealthController(@Value("${devpath.scaffold.health-status:ok}") String status) {
        this.status = status;
    }

    @GetMapping("/internal/health")
    Map<String, String> health() {
        return Map.of(
            "status", status,
            "application", "devpath-backend",
            "timestamp", Instant.now().toString()
        );
    }
}

