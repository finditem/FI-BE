package com.fmi.domain.user.user.web.swagger;

import com.fmi.domain.user.user.data.enums.UserOtherPageType;
import com.fmi.domain.user.user.web.dto.request.UserUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.UserMetaResponse;
import com.fmi.domain.user.user.web.dto.response.UserOtherPageResponse;
import com.fmi.domain.user.user.web.dto.response.UserProfileResponse;
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
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 정보 관리 API")
public interface UserSwagger {

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 프로필 정보를 조회합니다. (닉네임, 이메일, 프로필 이미지, 소셜 로그인 여부)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
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
    ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "약관 동의", description = "카카오 회원가입 후 약관 동의를 처리합니다. 개인정보 처리방침과 이용약관은 필수입니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약관 동의 성공"),
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
    ApiResponse<Void> agreeTerms(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.fmi.domain.user.user.web.dto.request.TermsAgreeRequest request);

    @Operation(summary = "유저 메타데이터 조회", description = "OG 태그 생성용 경량 API. 해당 유저의 닉네임을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메타데이터 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    ApiResponse<UserMetaResponse> getUserMeta(@PathVariable Long userId);

    @Operation(summary = "타인 페이지 조회", description = """
                다른 사용자의 닉네임, 프로필 이미지, 게시글, 작성 댓글, 즐겨찾기, 받은 후기 목록을 조회합니다.

                **type 파라미터:**
                - type=posts → 게시글만 조회
                - type=comments → 댓글만 조회
                - type=favorites → 즐겨찾기만 조회
                - type=reviews → 받은 후기만 조회 (최신 작성순, 받은 사용자가 숨긴 후기 포함)
                - type 미지정 → 기본 탭(posts) 조회

                **페이지네이션:**
                - cursor: 다음 페이지 시작점 (이전 응답의 nextCursor 값)
                - size: 한 페이지당 항목 수 (기본값 10)
                - 응답의 hasNext가 true이면 nextCursor로 다음 페이지 요청 가능
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "타인 페이지 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "USER400-PAGE_TYPE_INVALID: type 파라미터는 posts, comments, favorites, reviews 중 하나여야 합니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER400-PAGE_TYPE_INVALID\", \"message\": \"type 파라미터는 posts, comments, favorites, reviews 중 하나여야 합니다.\"}"))),
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
    ApiResponse<UserOtherPageResponse> getUserOtherPage(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "posts") UserOtherPageType type,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size);

    @Operation(summary = "내 정보 수정", description = """
                현재 로그인한 사용자의 프로필 정보를 수정합니다. (닉네임, 프로필 이미지 통합)

                **닉네임** (request JSON 내 nickname 필드):
                - 필드 자체를 보내지 않으면 → 닉네임 변경 없음
                - 값을 보내면 → 닉네임 변경 (2~10자, 한글·영문·숫자만 사용 가능)

                **프로필 이미지** (profileImage 파일 파트):
                - 파일을 보내지 않으면 → 이미지 변경 없음
                - 파일을 보내면 → 기존 이미지 삭제 후 새 이미지로 업데이트

                **이미지 삭제** (request JSON 내 deleteProfileImage 필드):
                - true로 보내면 → 기존 이미지 삭제 (프로필 이미지 없음 상태)
                - null 또는 필드 미포함 → 이미지 삭제 없음
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "AUTH409-NICKNAME_DUPLICATED: 이미 사용 중인 닉네임입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH409-NICKNAME_DUPLICATED\", \"message\": \"이미 사용 중인 닉네임입니다.\"}")))
    })
    ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "request", required = false) @Valid UserUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage);
}
