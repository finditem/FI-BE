package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminUserQueryService;
import com.fmi.domain.admin.web.dto.response.AdminDeletedUserResponse;
import com.fmi.domain.admin.web.dto.response.AdminUserDetailResponse;
import com.fmi.domain.admin.web.swagger.AdminUserSwagger;
import com.fmi.domain.auth.data.enums.WithdrawalReason;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController implements AdminUserSwagger {

    private final AdminUserQueryService adminUserQueryService;

    @GetMapping("/users/{userId}")
    @Override
    public ApiResponse<AdminUserDetailResponse> getUserDetail(@PathVariable Long userId) {
        AdminUserDetailResponse response = adminUserQueryService.getUserDetail(userId);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/users/deleted")
    @Override
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
