package com.fmi.domain.review.web.dto.response;

import com.fmi.domain.review.data.ReviewDetail;
import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewPostInfo;
import com.fmi.domain.review.web.dto.response.ReviewResponse.ReviewUserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewDetailResponse(
        Long reviewId,
        ReviewEmotion emotion,
        List<ReviewHelpType> helpTypes,
        @Schema(description = "상세 후기. 작성하지 않았으면 null") String content,
        LocalDateTime createdAt,
        @Schema(description = "후기를 보낸 사용자") ReviewUserResponse author,
        @Schema(description = "후기를 받은 사용자") ReviewUserResponse recipient,
        ReviewPostResponse post) {

    public static ReviewDetailResponse from(ReviewDetail detail) {
        return new ReviewDetailResponse(
                detail.reviewId(),
                detail.emotion(),
                detail.helpTypes().stream().sorted().toList(),
                detail.content(),
                detail.createdAt(),
                ReviewUserResponse.from(detail.author()),
                ReviewUserResponse.from(detail.recipient()),
                ReviewPostResponse.from(detail.post()));
    }

    public record ReviewPostResponse(
            Long postId,
            String title,
            String address,
            String thumbnailUrl,

            @Schema(description = "분실 일시. 게시글이 남아 있지 않으면 null")
            LocalDateTime date,

            @Schema(description = "게시글 작성일시. 게시글이 남아 있지 않으면 null")
            LocalDateTime createdAt,

            @Schema(description = "원본 게시글 삭제 여부") boolean deleted) {

        static ReviewPostResponse from(ReviewPostInfo post) {
            return new ReviewPostResponse(
                    post.postId(),
                    post.title(),
                    post.address(),
                    post.thumbnailUrl(),
                    post.date(),
                    post.createdAt(),
                    post.deleted());
        }
    }
}
