package br.com.sanquim.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
/** Endpoint público usado por verificações simples de disponibilidade. */
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "sanquim-api");
    }
}
