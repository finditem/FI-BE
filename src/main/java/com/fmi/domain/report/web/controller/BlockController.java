package com.fmi.domain.report.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.userblock.data.BlockedUser;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.global.apiPayload.ApiResponse;
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

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Report", description = "신고/차단 API")
public class BlockController {

    private final BlockService blockService;
    private final UserRepository userRepository;

    @PostMapping("/{userId}/block")
    @Operation(summary = "유저 차단", description = "로그인 사용자가 대상 유저를 차단합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "USER400-BLOCK_SELF: 자기 자신은 차단할 수 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "USER409-ALREADY_BLOCKED: 이미 차단한 사용자입니다")
    })
    public ApiResponse<String> block(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        blockService.block(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "유저 차단 해제", description = "로그인 사용자가 대상 유저의 차단을 해제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 차단 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_BLOCKED: 차단되지 않은 사용자입니다")
    })
    public ApiResponse<String> unblock(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        blockService.unblock(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @GetMapping("/block")
    @Operation(summary = "내가 차단한 유저 목록")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차단 유저 목록 조회 성공")
    })
    public ApiResponse<List<Long>> list(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        List<BlockedUser> blocks = blockService.list(user.getId());
        List<Long> ids = blocks.stream().map(b -> b.getBlocked().getId()).toList();
        return ApiResponse.onSuccess(ids);
    }
}


