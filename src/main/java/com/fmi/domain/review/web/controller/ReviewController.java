package com.fmi.domain.review.web.controller;

import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.service.ReviewQueryService;
import com.fmi.domain.review.service.ReviewService;
import com.fmi.domain.review.web.dto.request.ReviewCreateRequest;
import com.fmi.domain.review.web.dto.request.ReviewPageRequest;
import com.fmi.domain.review.web.dto.response.ReviewDetailResponse;
import com.fmi.domain.review.web.dto.response.ReviewIdResponse;
import com.fmi.domain.review.web.dto.response.ReviewPageResponse;
import com.fmi.domain.review.web.swagger.ReviewSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewSwagger {

    private final ReviewService reviewService;
    private final ReviewQueryService reviewQueryService;

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

    @Override
    @GetMapping("/users/me/reviews/received")
    public ApiResponse<ReviewPageResponse> getMyReceivedReviews(
            @Valid @ModelAttribute ReviewPageRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.onSuccess(ReviewPageResponse.from(reviewQueryService.getMyReceivedReviews(
                userDetails.getUsername(), request.getCursor(), request.getSize())));
    }

    @Override
    @GetMapping("/users/me/reviews/sent")
    public ApiResponse<ReviewPageResponse> getMySentReviews(
            @Valid @ModelAttribute ReviewPageRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        return ApiResponse.onSuccess(ReviewPageResponse.from(reviewQueryService.getMySentReviews(
                userDetails.getUsername(), request.getCursor(), request.getSize())));
    }

    @Override
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewDetailResponse> getReview(@PathVariable Long reviewId) {
        return ApiResponse.onSuccess(ReviewDetailResponse.from(reviewQueryService.getReview(reviewId)));
    }
}
