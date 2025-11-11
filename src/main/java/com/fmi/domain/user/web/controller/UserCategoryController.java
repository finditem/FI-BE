package com.fmi.domain.user.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.user.service.UserCategoryService;
import com.fmi.domain.user.web.dto.response.UserCategoryResponse;
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
@RequestMapping("/users/categories")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User", description = "사용자 카테고리 관리 API")
public class UserCategoryController {

    private final UserCategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "내 카테고리 목록",
            description = "로그인한 사용자가 구독 중인 게시글 카테고리를 조회합니다."
    )
    public ApiResponse<List<UserCategoryResponse>> list(@AuthenticationPrincipal User me) {
        return ApiResponse.onSuccess(
                categoryService.list(me.getId()).stream()
                        .map(UserCategoryResponse::from)
                        .toList()
        );
    }

    @GetMapping("/options")
    @Operation(
            summary = "전체 카테고리 목록",
            description = "게시글 작성 및 구독에 사용할 수 있는 모든 카테고리(Type) 값을 반환합니다."
    )
    public ApiResponse<List<Category>> availableCategories() {
        return ApiResponse.onSuccess(List.of(Category.values()));
    }

    @PostMapping
    @Operation(
            summary = "카테고리 추가",
            description = "요청 바디에 전달한 게시글 카테고리를 내 구독 목록에 추가합니다. 이미 존재하면 무시합니다."
    )
    public ApiResponse<String> add(@AuthenticationPrincipal User me, @RequestBody CategoryRequest req) {
        categoryService.add(me.getId(), req.getCategory());
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping
    @Operation(
            summary = "카테고리 삭제",
            description = "요청 바디에 전달한 게시글 카테고리를 내 구독 목록에서 제거합니다."
    )
    public ApiResponse<String> remove(@AuthenticationPrincipal User me, @RequestBody CategoryRequest req) {
        categoryService.remove(me.getId(), req.getCategory());
        return ApiResponse.onSuccess("OK");
    }

    @Data
    public static class CategoryRequest {
        private Category category;
    }
}


