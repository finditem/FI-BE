package com.fmi.domain.place.web.swagger;

import com.fmi.domain.place.web.dto.request.FavoritePlaceRequest;
import com.fmi.domain.place.web.dto.response.FavoritePlacePageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "장소", description = "장소 탐색, 장소 좋아요와 운영진 장소 관리를 제공합니다.")
public interface MyFavoritePlaceSwagger {

    @Operation(summary = "좋아요 장소 목록 조회", description = "현재 노출할 수 있는 좋아요 장소를 최신 좋아요순으로 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 장소 목록 조회 성공"))
    ApiResponse<FavoritePlacePageResponse> getFavorites(
            @Valid @ModelAttribute FavoritePlaceRequest request, @AuthenticationPrincipal UserDetails userDetails);
}
