package com.fmi.domain.place.web.swagger;

import com.fmi.domain.place.web.dto.request.FavoritePlaceRequest;
import com.fmi.domain.place.web.dto.response.FavoritePlacePageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "My Favorite Place", description = "가보고 싶은 장소 API")
public interface MyFavoritePlaceSwagger {

    @Operation(
            summary = "가보고 싶은 장소 목록 조회",
            description =
                    "현재 노출할 수 있는 저장 장소를 최신 저장순으로 조회합니다. totalCount는 커서와 페이지 크기에 관계없이 현재 노출 가능한 전체 좋아요 장소 수입니다. language는 KO 또는 EN이며 생략하면 회원 선호 언어와 관계없이 KO입니다. 빈 값과 소문자는 PLACE400-INVALID_REQUEST로 거절합니다. 미등록 영문 필드는 한국어로 제공합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 장소 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "PLACE400-INVALID_REQUEST: 유효하지 않은 조회 언어")
    })
    ApiResponse<FavoritePlacePageResponse> getFavorites(
            @ParameterObject @Valid @ModelAttribute FavoritePlaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails);
}
