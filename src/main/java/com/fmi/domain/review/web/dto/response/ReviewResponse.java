package com.fmi.domain.review.web.dto.response;

import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewPage;
import com.fmi.domain.review.data.ReviewSummary;
import com.fmi.domain.review.data.ReviewUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record ReviewResponse(
        Long reviewId,
        ReviewEmotion emotion,
        List<ReviewHelpType> helpTypes,
        @Schema(description = "상세 후기. 작성하지 않았으면 null") String content,
        LocalDateTime createdAt,
        @Schema(description = "받은 후기면 작성자, 보낸 후기면 받은 사용자") ReviewUserResponse user) {

    public static List<ReviewResponse> listFrom(ReviewPage page) {
        return page.reviews().stream()
                .map(summary -> from(summary, page.helpTypesByReviewId().getOrDefault(summary.reviewId(), Set.of())))
                .toList();
    }

    private static ReviewResponse from(ReviewSummary summary, Set<ReviewHelpType> helpTypes) {
        return new ReviewResponse(
                summary.reviewId(),
                summary.emotion(),
                helpTypes.stream().sorted().toList(),
                summary.content(),
                summary.createdAt(),
                ReviewUserResponse.from(summary.user()));
    }

    public record ReviewUserResponse(
            @Schema(description = "탈퇴한 사용자면 null") Long userId,
            @Schema(description = "탈퇴한 사용자면 null") String nickname,
            @Schema(description = "탈퇴한 사용자면 null") String profileImageUrl,
            boolean withdrawn) {

        static ReviewUserResponse from(ReviewUser user) {
            if (user.isWithdrawn()) {
                return new ReviewUserResponse(null, null, null, true);
            }
            return new ReviewUserResponse(user.userId(), user.nickname(), user.profileImageUrl(), false);
        }
    }
}
