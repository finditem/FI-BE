package com.fmi.domain.user.web.swagger;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.user.web.controller.UserCategoryController.CategoryRequest;
import com.fmi.domain.user.web.dto.response.UserCategoryResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 카테고리 관리 API")
public interface UserCategorySwagger {

    @Operation(summary = "내 카테고리 목록", description = "로그인한 사용자가 구독 중인 게시글 카테고리를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 목록 조회 성공")
    })
    ApiResponse<List<UserCategoryResponse>> list(@AuthenticationPrincipal UserDetails principal);

    @Operation(summary = "전체 카테고리 목록", description = "게시글 작성 및 구독에 사용할 수 있는 모든 카테고리(Type) 값을 반환합니다.")
    ApiResponse<List<Category>> availableCategories();

    @Operation(summary = "카테고리 추가", description = "요청 바디에 전달한 게시글 카테고리를 내 구독 목록에 추가합니다. 이미 존재하면 무시합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 추가 성공")
    })
    ApiResponse<String> add(@AuthenticationPrincipal UserDetails principal, @RequestBody CategoryRequest req);

    @Operation(summary = "카테고리 삭제", description = "요청 바디에 전달한 게시글 카테고리를 내 구독 목록에서 제거합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카테고리 삭제 성공")
    })
    ApiResponse<String> remove(@AuthenticationPrincipal UserDetails principal, @RequestBody CategoryRequest req);
}
