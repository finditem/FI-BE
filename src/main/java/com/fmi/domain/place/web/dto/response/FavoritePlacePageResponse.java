package com.fmi.domain.place.web.dto.response;

import com.fmi.domain.place.data.FavoritePlacePage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record FavoritePlacePageResponse(
        List<PlaceSummaryResponse> places,

        @Schema(description = "현재 노출 가능한 전체 좋아요 장소 수. 커서와 페이지 크기에 관계없이 반환", example = "3", minimum = "0")
        long totalCount,

        boolean hasNext,
        LocalDateTime nextFavoriteUpdatedAt) {

    public static FavoritePlacePageResponse from(FavoritePlacePage page) {
        return new FavoritePlacePageResponse(
                page.places().stream().map(PlaceSummaryResponse::from).toList(),
                page.totalCount(),
                page.hasNext(),
                page.nextFavoriteUpdatedAt());
    }
}
