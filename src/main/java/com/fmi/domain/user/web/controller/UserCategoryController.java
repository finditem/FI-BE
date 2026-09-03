package com.fmi.domain.user.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.user.service.UserCategoryService;
import com.fmi.domain.user.web.dto.response.UserCategoryResponse;
import com.fmi.domain.user.web.swagger.UserCategorySwagger;
import com.fmi.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/categories")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserCategoryController implements UserCategorySwagger {

    private final UserCategoryService categoryService;

    @GetMapping
    @Override
    public ApiResponse<List<UserCategoryResponse>> list(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.onSuccess(categoryService.list(principal.getUsername()).stream()
                .map(UserCategoryResponse::from)
                .toList());
    }

    @GetMapping("/options")
    @Override
    public ApiResponse<List<Category>> availableCategories() {
        return ApiResponse.onSuccess(List.of(Category.values()));
    }

    @PostMapping
    @Override
    public ApiResponse<String> add(@AuthenticationPrincipal UserDetails principal, @RequestBody CategoryRequest req) {
        categoryService.add(principal.getUsername(), req.getCategory());
        return ApiResponse.onSuccess("OK");
    }

    @DeleteMapping
    @Override
    public ApiResponse<String> remove(
            @AuthenticationPrincipal UserDetails principal, @RequestBody CategoryRequest req) {
        categoryService.remove(principal.getUsername(), req.getCategory());
        return ApiResponse.onSuccess("OK");
    }

    @Data
    public static class CategoryRequest {
        private Category category;
    }
}
