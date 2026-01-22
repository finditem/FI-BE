package com.fmi.domain.inquirycomment.web.controller;

import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.response.InquiryCommentSliceResponse;
import com.fmi.domain.inquirycomment.service.InquiryCommentService;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inquiries/{inquiryId}/comments")
@RequiredArgsConstructor
@Tag(name = "InquiryComment", description = "문의 댓글 관련 API")
public class InquiryCommentController {

    private final InquiryCommentService inquiryCommentService;

    @PostMapping
    @Operation(summary = "문의 댓글 작성", description = "문의 작성자 또는 관리자만 댓글 작성 가능. 비회원인 경우 email 파라미터 필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 1, \"content\": \"댓글 내용\", \"authorId\": 1, \"authorName\": \"닉네임\", \"authorEmail\": \"user@example.com\", \"parentId\": null, \"replies\": [], \"createdAt\": \"2024-01-01T00:00:00\", \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> createComment(
            @RequestBody CreateInquiryCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @RequestParam(required = false) String email) {

        InquiryCommentResponse response = inquiryCommentService.createComment(request, userDetails, inquiryId, email);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping
    @Operation(summary = "문의 댓글 목록 조회 (커서 기반 무한스크롤)", description = "문의 작성자 또는 관리자만 조회 가능. 비회원인 경우 email 파라미터 필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"comments\": [{\"id\": 1, \"content\": \"댓글 내용\", \"authorId\": 1, \"authorName\": \"닉네임\", \"authorEmail\": \"user@example.com\", \"parentId\": null, \"replies\": [], \"createdAt\": \"2024-01-01T00:00:00\", \"canEdit\": false, \"canDelete\": true}], \"hasNext\": true, \"cursor\": 1}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다")
    })
    public ResponseEntity<ApiResponse<InquiryCommentSliceResponse>> getComments(
            @PathVariable Long inquiryId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String email) {

        InquiryCommentSliceResponse response = inquiryCommentService.getComments(inquiryId, cursor, size, userDetails, email);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "문의 댓글 수정", description = "작성자만 수정할 수 있습니다. 비회원 댓글은 수정 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 1, \"content\": \"수정된 댓글\", \"authorId\": 1, \"authorName\": \"닉네임\", \"authorEmail\": \"user@example.com\", \"parentId\": null, \"replies\": [], \"createdAt\": \"2024-01-01T00:00:00\", \"canEdit\": true, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> updateComment(
            @RequestBody CreateInquiryCommentDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId,
            @RequestParam(required = false) String email) {

        InquiryCommentResponse response = inquiryCommentService.updateComment(request, userDetails, commentId, email);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "문의 댓글 삭제", description = "작성자 또는 관리자(ROLE_ADMIN)만 삭제할 수 있습니다. 관리자는 다른 사람의 댓글도 삭제 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "댓글 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"id\": 1, \"content\": \"삭제된 댓글\", \"authorId\": 1, \"authorName\": \"닉네임\", \"authorEmail\": \"user@example.com\", \"parentId\": null, \"replies\": [], \"createdAt\": \"2024-01-01T00:00:00\", \"canEdit\": false, \"canDelete\": true}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId,
            @RequestParam(required = false) String email) {

        InquiryCommentResponse response = inquiryCommentService.deleteComment(userDetails, commentId, email);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
