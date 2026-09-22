package com.fmi.domain.report.web.controller;

import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.domain.userblock.web.dto.response.BlockedUserResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "차단", description = "회원 사이의 차단 관계를 관리합니다.")
public class BlockController {

    private final BlockService blockService;
    private final UserRepository userRepository;

    @PostMapping("/{userId}/block")
    @Operation(summary = "사용자 차단", description = "현재 회원과 대상 회원 사이에 차단 관계를 설정합니다. 차단된 회원과는 메시지를 주고받을 수 없습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 차단 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "USER400-BLOCK_SELF: 자기 자신은 차단할 수 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "USER409-ALREADY_BLOCKED: 이미 차단한 사용자입니다")
    })
    public ApiResponse<String> block(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        blockService.block(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "사용자 차단 해제", description = "현재 회원과 대상 회원 사이의 차단 관계를 해제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 차단 해제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_BLOCKED: 차단되지 않은 사용자입니다")
    })
    public ApiResponse<String> unblock(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        blockService.unblock(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @GetMapping("/block")
    @Operation(summary = "차단한 사용자 목록 조회", description = "현재 회원의 차단 목록을 커서 방식으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차단한 사용자 목록 조회 성공")
    })
    public ApiResponse<CursorPageResponse<BlockedUserResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        CursorPageResponse<BlockedUserResponse> blockedUsers =
                blockService.listWithUserInfoCursor(user.getId(), cursor, size);
        return ApiResponse.onSuccess(blockedUsers);
    }
}
