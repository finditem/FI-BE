package com.fmi.domain.postfavorite.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.postfavorite.response.PostFavoriteResponse;
import com.fmi.domain.postfavorite.service.PostFavoriteService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 관련 API")
public class PostFavoriteController {
    private final PostFavoriteService postFavoriteService;
    private final PostQueryService postQueryService;

    @Operation(
            summary = "즐겨찾기 추가",
            description = "즐겨찾기를 추가합니다.",
            tags = {"Post"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다")
    })
    @PostMapping("/posts/{postId}/favorites")
    public ResponseEntity<ApiResponse<PostFavoriteResponse>> createFavorite(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {

        PostFavoriteResponse response = postFavoriteService.addFavorite(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
            summary = "즐겨찾기 삭제",
            description = "즐겨찾기를 삭제합니다.",
            tags = {"Post"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "_POST_FAVORITE_NOT_FOUND: 해당 게시글에 대한 즐겨찾기를 하지 않았습니다.")
    })
    @DeleteMapping("/posts/{postId}/favorites")
    public ResponseEntity<ApiResponse<PostFavoriteResponse>> deleteFavorite(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        PostFavoriteResponse response = postFavoriteService.deleteFavorite(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
            summary = "즐겨찾기 목록 조회",
            description = """
            현재 로그인한 사용자의 즐겨찾기 목록을 커서 기반으로 조회합니다.

            **필터 파라미터** (모두 선택):
            - postType: LOST / FOUND
            - category: ELECTRONICS / WALLET / ID_CARD / JEWELRY / BAG / CARD / ETC
            - address: 지역 필터 (예: 서울특별시, 서울특별시 강남구)
            - keyword: 제목 또는 내용 검색
            """,
            tags = {"User"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공")
    })
    @GetMapping("/users/me/favorites")
    public ResponseEntity<ApiResponse<PostPageResponse>> getFavoritePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") int size) {
        PostPageResponse response = postQueryService.getFavoritePost(
                userDetails, postType, category, address, keyword, sortType, cursor, size);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
