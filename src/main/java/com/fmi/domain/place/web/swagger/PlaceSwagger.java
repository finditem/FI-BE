package com.fmi.domain.place.web.swagger;

import com.fmi.domain.place.data.enums.PlaceType;
import com.fmi.domain.place.web.dto.response.HomePlaceResponse;
import com.fmi.domain.place.web.dto.response.PlaceFavoriteResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "장소", description = "장소 탐색, 장소 좋아요와 운영진 장소 관리를 제공합니다.")
public interface PlaceSwagger {

    @Operation(summary = "장소 목록 조회", description = "지도 카테고리를 선택하지 않은 화면에 노출할 최신 장소를 최대 5개 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "홈 장소 조회 성공"))
    ApiResponse<HomePlaceResponse> getHomePlaces(
            @RequestParam(required = false) PlaceType type, @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "장소 좋아요 추가", description = "노출 가능한 장소에 좋아요를 추가합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "장소 좋아요 추가 성공"))
    ApiResponse<PlaceFavoriteResponse> saveFavorite(
            @PathVariable Long placeId, @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "장소 좋아요 취소", description = "장소 좋아요를 취소합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "장소 좋아요 취소 성공"))
    ApiResponse<PlaceFavoriteResponse> cancelFavorite(
            @PathVariable Long placeId, @AuthenticationPrincipal UserDetails userDetails);
}
