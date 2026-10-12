package com.fmi.domain.review.web.dto.request;

import com.fmi.domain.review.data.ReviewListType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewPageRequest {

    @Schema(description = "RECEIVED(받은 후기), SENT(보낸 후기). 기본값 RECEIVED")
    @NotNull private ReviewListType type = ReviewListType.RECEIVED;

    @Schema(description = "이전 응답의 nextCursor. 첫 페이지는 생략합니다.")
    private Long cursor;

    @Schema(description = "조회 개수. 1~50, 기본값 10")
    @Min(1) @Max(50) private int size = 10;
}
