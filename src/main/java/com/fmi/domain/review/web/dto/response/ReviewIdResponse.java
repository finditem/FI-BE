package com.fmi.domain.review.web.dto.response;

import com.fmi.domain.review.data.Review;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewIdResponse(
        @Schema(description = "작성한 후기 id") Long reviewId) {

    public static ReviewIdResponse from(Review review) {
        return new ReviewIdResponse(review.getId());
    }
}
