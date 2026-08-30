package com.devpath.platform.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InternalHealthController {
    @GetMapping("/internal/health")
    Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "application", "devpath-backend",
            "timestamp", Instant.now().toString()
        );
    }
}
