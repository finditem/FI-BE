package com.fmi.domain.postfavorite.web.controller;

import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.postfavorite.service.PostFavoriteService;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 관련 API")
public class PostFavoriteController {
    private final PostFavoriteService postFavoriteService;

    @PutMapping("/post/{postId}/favorites")
    @Operation(
            summary = "게시글 즐겨찾기(좋아요) 토글",
            description = """
                    로그인 사용자가 특정 게시글을 즐겨찾기(좋아요) 처리/취소합니다.
                    
                    동작:
                    - 해당 게시글에 대한 즐겨찾기 기록이 없으면 새로 생성 후 즐겨찾기 처리
                    - 기록이 있으면 isFavorite 값을 토글(true ↔ false)
                    
                    응답:
                    - 성공 시 204 No Content (응답 바디 없음)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "토글 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = @Content)
    })
    public ResponseEntity<Void> toggleFavorite(@PathVariable Long postId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        postFavoriteService.togglePostFavorite(userDetails, postId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "즐겨찾기 목록 조회", tags = {"User"})
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공")
    })
    @GetMapping("/users/me/favorites")
    public ResponseEntity<ApiResponse<List<PostBriefResponse>>> getFavoritePost(@AuthenticationPrincipal UserDetails userDetails) {

        List<PostBriefResponse> response = postFavoriteService.getFavoritePost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
