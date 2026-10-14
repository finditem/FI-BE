package com.fmi.domain.user.web.controller;

import com.fmi.domain.user.service.UserStatisticsService;
import com.fmi.domain.user.web.dto.response.UserStatisticsResponse;
import com.fmi.domain.user.web.swagger.UserStatisticsSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users/statistics")
@RequiredArgsConstructor
public class UserStatisticsController implements UserStatisticsSwagger {
    private final UserStatisticsService service;

    @Override
    @GetMapping
    public ApiResponse<UserStatisticsResponse> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var result = service.get(startDate, endDate);
        var response = UserStatisticsResponse.from(result);
        return ApiResponse.onSuccess(response);
    }
}
