package com.fmi.domain.inquiry.comment.web.swagger;

import com.fmi.domain.inquiry.comment.web.dto.request.InquiryCommentCreateRequest;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentResponse;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentSliceResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "InquiryComment", description = "문의 댓글 관련 API")
public interface InquiryCommentSwagger {

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
    ResponseEntity<ApiResponse<InquiryCommentResponse>> createComment(
            InquiryCommentCreateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails,
            Long inquiryId);

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
    ResponseEntity<ApiResponse<InquiryCommentSliceResponse>> getComments(
            Long inquiryId, Long cursor, int size, @Parameter(hidden = true) UserDetails userDetails);

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
    ResponseEntity<ApiResponse<InquiryCommentResponse>> updateComment(
            InquiryCommentCreateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails,
            Long inquiryId,
            Long commentId);

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
    ResponseEntity<ApiResponse<InquiryCommentResponse>> deleteComment(
            @Parameter(hidden = true) UserDetails userDetails, Long inquiryId, Long commentId);
}
