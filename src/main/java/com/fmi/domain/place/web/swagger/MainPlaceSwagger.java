package com.fmi.domain.place.web.swagger;

import com.fmi.domain.map.web.dto.response.PostMarkerResponse;
import com.fmi.domain.place.web.dto.request.NearbyPostMarkerRequest;
import com.fmi.domain.place.web.dto.request.NearbyPostRequest;
import com.fmi.domain.place.web.dto.request.PlaceMapSearchRequest;
import com.fmi.domain.place.web.dto.response.NearbyPostResponse;
import com.fmi.domain.place.web.dto.response.PlaceMapResponse;
import com.fmi.domain.place.web.dto.response.PlaceSummaryResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "장소", description = "장소 탐색, 장소 좋아요와 운영진 장소 관리를 제공합니다.")
public interface MainPlaceSwagger {

    @Operation(summary = "지도 범위 내 장소 목록 조회", description = "현재 지도 범위 안의 장소 마커와 장소 목록을 함께 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지도 장소 조회 성공"))
    ApiResponse<PlaceMapResponse> searchLocation(
            @Valid @ModelAttribute PlaceMapSearchRequest request, @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "장소 단일 요약 조회", description = "placeId로 장소 한 건의 화면 표시용 요약 정보를 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "장소 단일 요약 조회 성공"))
    ApiResponse<PlaceSummaryResponse> getSummary(
            @PathVariable Long placeId, @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "장소 주변 분실 및 발견 게시글 목록 조회", description = "장소 반경 500미터의 분실 및 발견 게시글을 거리 커서 방식으로 10개씩 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주변 게시글 조회 성공"))
    ApiResponse<NearbyPostResponse> getNearbyPosts(
            @PathVariable Long placeId,
            @Valid @ModelAttribute NearbyPostRequest request,
            @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "장소 주변 분실 및 발견 게시글 마커 조회", description = "장소 반경 500미터의 분실 및 발견 게시글 마커를 최대 10개 조회합니다.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주변 게시글 마커 조회 성공"))
    ApiResponse<List<PostMarkerResponse>> getNearbyPostMarkers(
            @PathVariable Long placeId,
            @Valid @ModelAttribute NearbyPostMarkerRequest request,
            @AuthenticationPrincipal UserDetails userDetails);
}
