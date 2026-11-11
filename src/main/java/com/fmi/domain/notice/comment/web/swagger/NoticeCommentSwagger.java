package com.fmi.domain.notice.comment.web.swagger;

import com.fmi.domain.notice.comment.web.dto.request.NoticeCommentCreateRequest;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentPageResponse;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentResponse;
import com.fmi.domain.notice.comment.web.dto.response.NoticeCommentSliceResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "NoticeComment", description = "공지사항 댓글 API")
public interface NoticeCommentSwagger {

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
    ResponseEntity<ApiResponse<NoticeCommentResponse>> createComment(
            Long noticeId,
            NoticeCommentCreateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails);

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
    ResponseEntity<ApiResponse<NoticeCommentSliceResponse>> getComments(
            Long noticeId, Long cursor, int size, @Parameter(hidden = true) UserDetails userDetails);

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
    ResponseEntity<ApiResponse<NoticeCommentPageResponse>> getReplies(
            Long commentId, int page, @Parameter(hidden = true) UserDetails userDetails);

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
    ResponseEntity<ApiResponse<NoticeCommentResponse>> updateComment(
            Long commentId,
            NoticeCommentCreateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails);

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
    ResponseEntity<ApiResponse<Void>> deleteComment(Long commentId, @Parameter(hidden = true) UserDetails userDetails);
}
