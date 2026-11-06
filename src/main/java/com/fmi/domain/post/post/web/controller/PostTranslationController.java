package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.service.PostTranslationService;
import com.fmi.domain.post.post.web.dto.response.PostTranslationResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostTranslationController {

    private final PostTranslationService postTranslationService;

    @GetMapping("/{postId}/translate")
    @Operation(summary = "게시글 번역 조회", description = """
                    게시글 제목/본문을 조회자의 선호 언어로 번역해 반환합니다.
                    원문 언어는 DeepL이 자동 감지하며, 대상 언어는 조회자의 선호 언어입니다.
                    선호 언어가 설정되어 있지 않은 사용자는 한국어(KO)로 취급합니다.

                    - 최초 요청 시 번역 후 결과를 저장하며, 이후 같은 게시글·대상 언어 요청은 저장된 값을 반환합니다.
                    - 게시글 내용이 실질적으로 변경되면 다음 요청 시 재번역됩니다.
                      특수문자나 띄어쓰기/줄바꿈만 바뀐 경우는 정규화 후 비교하여 실질적 변경으로 보지 않고 저장된 번역을 가져옵니다.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게시글 번역 조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = PostTranslationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다",
                content = @Content)
    })
    public ResponseEntity<ApiResponse<PostTranslationResponse>> translatePost(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {

        PostTranslationResponse response = postTranslationService.getOrTranslate(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
