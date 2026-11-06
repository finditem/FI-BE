package com.fmi.domain.user.user.web.swagger;

import com.fmi.domain.user.user.web.dto.request.PreferredLanguageUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.PreferredLanguageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 정보 관리 API")
public interface UserLanguageSwagger {

    @Operation(summary = "선호 언어 조회", description = "현재 로그인한 사용자의 선호 언어를 조회합니다. (KO: 한국어, EN: 영어)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "선호 언어 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}")))
    })
    ApiResponse<PreferredLanguageResponse> getPreferredLanguage(@AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "선호 언어 변경", description = """
                현재 로그인한 사용자의 선호 언어를 변경합니다. 게시글 번역 등 언어별 기능에 적용됩니다.

                **preferredLanguage** (필수): KO(한국어) / EN(영어)
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "선호 언어 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "USER400-PREFERRED_LANGUAGE_REQUIRED: 선호 언어는 필수입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER400-PREFERRED_LANGUAGE_REQUIRED\", \"message\": \"선호 언어는 필수입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}")))
    })
    ApiResponse<PreferredLanguageResponse> updatePreferredLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PreferredLanguageUpdateRequest request);
}
