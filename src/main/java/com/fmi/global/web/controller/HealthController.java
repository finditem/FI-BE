package com.fmi.global.web.controller;

import com.fmi.global.web.swagger.HealthSwagger;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthSwagger {

    @GetMapping("/health")
    @Override
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
