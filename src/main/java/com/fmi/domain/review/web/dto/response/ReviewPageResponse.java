package com.fmi.domain.review.web.dto.response;

import com.fmi.domain.review.data.ReviewPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ReviewPageResponse(
        List<ReviewResponse> reviewList,
        @Schema(description = "목록 조건에 맞는 전체 후기 수") long reviewCount,
        Long nextCursor,
        boolean hasNext) {

    public static ReviewPageResponse from(ReviewPage page) {
        return new ReviewPageResponse(
                ReviewResponse.listFrom(page), page.totalCount(), page.nextCursor(), page.hasNext());
    }
}
