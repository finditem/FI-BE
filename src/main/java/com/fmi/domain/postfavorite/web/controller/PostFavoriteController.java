package com.fmi.domain.postfavorite.web.controller;

import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.postfavorite.response.PostFavoriteResponse;
import com.fmi.domain.postfavorite.service.PostFavoriteService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostFavoriteController {

    private final PostFavoriteService postFavoriteService;

    @Operation(summary = "즐겨찾기 추가",description = "토글형식 - true: 즐겨찾기 추가, false: 해제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    @PostMapping("/{postId}/favorite")
    public ResponseEntity<ApiResponse<PostFavoriteResponse>> toggleFavorite(@PathVariable Long postId,
                                                              @AuthenticationPrincipal UserDetails userDetails) {

        boolean isFavorite = postFavoriteService.toggleFavorite(postId, userDetails);
        PostFavoriteResponse response = new PostFavoriteResponse(postId, isFavorite);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "즐겨찾기 목록 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    @GetMapping("/post/favoriteList")
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getFavoritePost(@AuthenticationPrincipal UserDetails userDetails){

        List<PostListResponse> response = postFavoriteService.getFavoritePost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
