package com.fmi.domain.review.web.controller;

import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.service.ReviewService;
import com.fmi.domain.review.web.dto.request.ReviewCreateRequest;
import com.fmi.domain.review.web.dto.response.ReviewIdResponse;
import com.fmi.domain.review.web.swagger.ReviewSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewSwagger {

    private final ReviewService reviewService;

    @Override
    @PostMapping("/find-completions/{findCompletionId}/review")
    public ApiResponse<ReviewIdResponse> write(
            @PathVariable Long findCompletionId,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Review review = reviewService.write(
                findCompletionId, userDetails.getUsername(), request.emotion(), request.helpTypes(), request.content());
        return ApiResponse.onSuccess(ReviewIdResponse.from(review));
    }

    @Override
    @PostMapping("/reviews/{reviewId}/hide")
    public ApiResponse<Void> hide(@PathVariable Long reviewId, @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.hide(reviewId, userDetails.getUsername());
        return ApiResponse.onSuccess(null);
    }
}
