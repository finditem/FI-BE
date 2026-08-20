package com.fmi.domain.inquirycomment.web.controller;

import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.response.InquiryCommentSliceResponse;
import com.fmi.domain.inquirycomment.service.InquiryCommentService;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/inquiries/{inquiryId}/comments")
@RequiredArgsConstructor
@Tag(name = "InquiryComment", description = "문의 댓글 관련 API")
public class InquiryCommentController {

    private final InquiryCommentService inquiryCommentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "문의 댓글 작성", description = "문의 작성자 또는 관리자만 댓글 작성 가능. 이미지 첨부 가능.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "COMMON401: 인증이 필요합니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> createComment(
            @RequestPart("comment") CreateInquiryCommentDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId) {

        InquiryCommentResponse response = inquiryCommentService.createComment(request, images, userDetails, inquiryId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping
    @Operation(summary = "문의 댓글 목록 조회 (커서 기반 무한스크롤)", description = "문의 작성자 또는 관리자만 조회 가능.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다")
    })
    public ResponseEntity<ApiResponse<InquiryCommentSliceResponse>> getComments(
            @PathVariable Long inquiryId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        InquiryCommentSliceResponse response = inquiryCommentService.getComments(inquiryId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "문의 댓글 수정", description = "작성자만 수정할 수 있습니다. 이미지 첨부 가능.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "COMMON403: 금지된 요청입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> updateComment(
            @RequestPart("comment") CreateInquiryCommentDto request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId) {

        InquiryCommentResponse response = inquiryCommentService.updateComment(request, images, userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "문의 댓글 삭제", description = "작성자 또는 관리자(ROLE_ADMIN)만 삭제할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "COMMON403: 금지된 요청입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "COMMENT404-NOT_FOUND: 존재하지 않는 댓글입니다")
    })
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId) {

        InquiryCommentResponse response = inquiryCommentService.deleteComment(userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
