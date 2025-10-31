package com.fmi.domain.user.web.controller;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.user.data.UserKeyword;
import com.fmi.domain.user.service.UserKeywordService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/keywords")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User", description = "사용자 키워드 관리 API")
public class UserKeywordController {

    private final UserKeywordService keywordService;

    @GetMapping
    @Operation(summary = "내 키워드 목록")
    public ApiResponse<List<UserKeyword>> list(@AuthenticationPrincipal User me) {
        return ApiResponse.onSuccess(keywordService.list(me.getId()));
    }

    @PostMapping
    @Operation(summary = "키워드 추가")
    public ApiResponse<String> add(@AuthenticationPrincipal User me, @RequestBody KeywordRequest req) {
        keywordService.add(me.getId(), req.getCategory(), req.getKeyword());
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping
    @Operation(summary = "키워드 삭제")
    public ApiResponse<String> remove(@AuthenticationPrincipal User me, @RequestBody KeywordRequest req) {
        keywordService.remove(me.getId(), req.getCategory(), req.getKeyword());
        return ApiResponse.onSuccess("OK");
    }

    @Data
    public static class KeywordRequest {
        private Type category;
        private String keyword;
    }
}


