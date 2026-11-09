package com.fmi.domain.user.block.web.controller;

import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.block.web.dto.response.BlockedUserResponse;
import com.fmi.domain.user.block.web.swagger.BlockSwagger;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.service.UserQueryService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BlockController implements BlockSwagger {

    private final BlockService blockService;
    private final UserQueryService userQueryService;

    @PostMapping("/{userId}/block")
    @Override
    public ApiResponse<String> block(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userQueryService.findUser(userDetails.getUsername());
        blockService.block(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping("/{userId}/block")
    @Override
    public ApiResponse<String> unblock(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long userId) {
        User user = userQueryService.findUser(userDetails.getUsername());
        blockService.unblock(user.getId(), userId);
        return ApiResponse.onSuccess("OK");
    }

    @GetMapping("/block")
    @Override
    public ApiResponse<CursorPageResponse<BlockedUserResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        User user = userQueryService.findUser(userDetails.getUsername());
        CursorPageResponse<BlockedUserResponse> blockedUsers =
                blockService.listWithUserInfoCursor(user.getId(), cursor, size);
        return ApiResponse.onSuccess(blockedUsers);
    }
}
