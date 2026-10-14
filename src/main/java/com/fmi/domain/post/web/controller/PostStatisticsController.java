package com.fmi.domain.post.web.controller;

import com.fmi.domain.post.service.PostStatisticsService;
import com.fmi.domain.post.web.dto.response.PostStatisticsResponse;
import com.fmi.domain.post.web.swagger.PostStatisticsSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/posts/statistics")
@RequiredArgsConstructor
public class PostStatisticsController implements PostStatisticsSwagger {
    private final PostStatisticsService service;

    @Override
    @GetMapping
    public ApiResponse<PostStatisticsResponse> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var result = service.get(startDate, endDate);
        var response = PostStatisticsResponse.from(result);
        return ApiResponse.onSuccess(response);
    }
}
