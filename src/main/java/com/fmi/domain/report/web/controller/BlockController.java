package com.fmi.domain.report.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.userblock.data.BlockedUser;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Report", description = "신고/차단 API")
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{userId}/block")
    @Operation(summary = "유저 차단", description = "로그인 사용자가 대상 유저를 차단합니다.")
    public ApiResponse<String> block(@AuthenticationPrincipal User me, @PathVariable Long userId) {
        blockService.block(me.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "유저 차단 해제", description = "로그인 사용자가 대상 유저의 차단을 해제합니다.")
    public ApiResponse<String> unblock(@AuthenticationPrincipal User me, @PathVariable Long userId) {
        blockService.unblock(me.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @GetMapping("/block")
    @Operation(summary = "내가 차단한 유저 목록")
    public ApiResponse<List<Long>> list(@AuthenticationPrincipal User me) {
        List<BlockedUser> blocks = blockService.list(me.getId());
        List<Long> ids = blocks.stream().map(b -> b.getBlocked().getId()).toList();
        return ApiResponse.onSuccess(ids);
    }
}


