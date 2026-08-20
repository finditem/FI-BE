package com.fmi.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "헬스체크용 API")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
