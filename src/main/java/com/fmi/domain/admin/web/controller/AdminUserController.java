package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminUserQueryService;
import com.fmi.domain.admin.web.dto.response.AdminDeletedUserResponse;
import com.fmi.domain.admin.web.dto.response.AdminUserDetailResponse;
import com.fmi.domain.auth.data.enums.WithdrawalReason;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminUserController {

    private final AdminUserQueryService adminUserQueryService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "관리자 사용자 상세 조회", description = "특정 사용자의 기본 정보 및 활동 통계를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 상세 조회 성공"),
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
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponse response = adminUserQueryService.getUserDetail(userId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/users/deleted")
    @Operation(summary = "탈퇴 유저 목록 조회", description = "커서 기반 페이지네이션으로 탈퇴한 사용자 목록을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 유저 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<AdminDeletedUserResponse>> getDeletedUsers(
            @RequestParam(required = false) WithdrawalReason reason,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorPageResponse<AdminDeletedUserResponse> response =
                adminUserQueryService.getDeletedUsersCursorPage(reason, keyword, cursor, size);
        return ApiResponse.onSuccess(response);
    }
}
