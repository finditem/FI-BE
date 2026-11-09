package com.fmi.domain.inquiry.comment.web.controller;

import com.fmi.domain.inquiry.comment.service.InquiryCommentService;
import com.fmi.domain.inquiry.comment.web.dto.request.InquiryCommentCreateRequest;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentResponse;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentSliceResponse;
import com.fmi.domain.inquiry.comment.web.swagger.InquiryCommentSwagger;
import com.fmi.global.apiPayload.ApiResponse;
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
public class InquiryCommentController implements InquiryCommentSwagger {

    private final InquiryCommentService inquiryCommentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> createComment(
            @RequestPart("comment") InquiryCommentCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId) {

        InquiryCommentResponse response = inquiryCommentService.createComment(request, images, userDetails, inquiryId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping
    @Override
    public ResponseEntity<ApiResponse<InquiryCommentSliceResponse>> getComments(
            @PathVariable Long inquiryId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        InquiryCommentSliceResponse response = inquiryCommentService.getComments(inquiryId, cursor, size, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> updateComment(
            @RequestPart("comment") InquiryCommentCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId) {

        InquiryCommentResponse response = inquiryCommentService.updateComment(request, images, userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{commentId}")
    @Override
    public ResponseEntity<ApiResponse<InquiryCommentResponse>> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long inquiryId,
            @PathVariable Long commentId) {

        InquiryCommentResponse response = inquiryCommentService.deleteComment(userDetails, commentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
