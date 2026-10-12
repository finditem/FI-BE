package com.fmi.domain.review.web.dto.response;

import com.fmi.domain.review.data.Review;

public record ReviewIdResponse(Long reviewId) {

    public static ReviewIdResponse from(Review review) {
        return new ReviewIdResponse(review.getId());
    }
}
