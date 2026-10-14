package com.fmi.domain.temperature.web.controller;

import com.fmi.domain.temperature.service.TemperatureQueryService;
import com.fmi.domain.temperature.web.dto.response.TemperatureResponse;
import com.fmi.domain.temperature.web.swagger.TemperatureSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TemperatureController implements TemperatureSwagger {

    private final TemperatureQueryService temperatureQueryService;

    @Override
    @GetMapping("/users/me/temperature")
    public ApiResponse<TemperatureResponse> getMyTemperature(@AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.onSuccess(
                TemperatureResponse.from(temperatureQueryService.getMyTemperature(userDetails.getUsername())));
    }
}
