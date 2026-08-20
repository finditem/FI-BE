package com.fmi.domain.noticecomment.web.controller;

import com.fmi.domain.noticecomment.response.NoticeCommentPageResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentSliceResponse;
import com.fmi.domain.noticecomment.service.NoticeCommentService;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Tag(name = "NoticeComment", description = "공지사항 댓글 API")
public class NoticeCommentController {

    private final NoticeCommentService noticeCommentService;

    @PostMapping(value = "/{noticeId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "공지사항 댓글 생성", description = "인증된 사용자(관리자 포함)만 작성할 수 있습니다. 이미지 첨부 가능.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}")))
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            @PathVariable Long noticeId,
            @RequestPart("request") @Valid CreateNoticeCommentDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.createComment(noticeId, request, images, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{noticeId}/comments")
    @Operation(summary = "공지사항 댓글 조회 (커서 기반 무한스크롤)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NOTICE404-NOT_FOUND\", \"message\": \"존재하지 않는 공지사항입니다.\"}")))
    })
    public ResponseEntity<ApiResponse<NoticeCommentSliceResponse>> getComments(
            @PathVariable Long noticeId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentSliceResponse response = noticeCommentService.getComments(noticeId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(
            summary = "공지사항 대댓글 조회 (페이지네이션)",
            description = "특정 댓글의 대댓글 목록을 페이지네이션 방식으로 조회합니다. "
                    + "첫 요청은 page=0, '더보기' 클릭 시 nextPage 값을 page로 넣어 다음 페이지를 요청합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대댓글 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}")))
    })
    public ResponseEntity<ApiResponse<NoticeCommentPageResponse>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentPageResponse response = noticeCommentService.getReplies(commentId, page, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/comments/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "공지사항 댓글 수정", description = "작성자만 수정할 수 있습니다. 이미지 재업로드 시 기존 이미지는 삭제됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}")))
    })
    public ResponseEntity<ApiResponse<NoticeCommentResponse>> updateComment(
            @PathVariable Long commentId,
            @RequestPart("request") @Valid CreateNoticeCommentDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        NoticeCommentResponse response = noticeCommentService.updateComment(commentId, request, images, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "공지사항 댓글 추천 추가")
    public ResponseEntity<ApiResponse<Void>> addCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentService.addCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "공지사항 댓글 추천 취소")
    public ResponseEntity<ApiResponse<Void>> removeCommentLike(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        noticeCommentService.removeCommentLike(commentId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "공지사항 댓글 삭제", description = "작성자 또는 관리자(ROLE_ADMIN)만 삭제할 수 있습니다. 대댓글이 있으면 소프트 삭제됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"COMMENT404-NOT_FOUND\", \"message\": \"존재하지 않는 댓글입니다.\"}")))
    })
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {

        noticeCommentService.deleteComment(commentId, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
