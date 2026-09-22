package com.fmi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "시스템", description = "서비스 상태와 파일 저장소 연동을 확인합니다.")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "서비스 상태 확인", description = "서비스가 요청을 처리할 수 있는 상태인지 확인합니다.")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
