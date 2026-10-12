package com.fmi.domain.review.web.swagger;

import com.fmi.domain.review.web.dto.request.ReviewCreateRequest;
import com.fmi.domain.review.web.dto.response.ReviewIdResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Review", description = "후기 API")
public interface ReviewSwagger {

    @Operation(
            summary = "후기 작성",
            description = "찾기 완료를 처리한 분실물 게시글 작성자가 도움을 준 사용자에게 후기를 작성합니다. 찾기 완료 건마다 1회만 작성할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "후기 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "감정 유형 또는 도움 경험이 없습니다. REVIEW400-INVALID_CONTENT: 상세 후기가 300자를 넘습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "REVIEW403-ACCESS_DENIED: 찾기 완료를 처리한 사용자가 아닙니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "FIND_COMPLETION404-NOT_FOUND: 찾기 완료 기록이 없습니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "REVIEW409-ALREADY_WRITTEN: 이미 후기를 작성했습니다.")
    })
    ApiResponse<ReviewIdResponse> write(
            @PathVariable Long findCompletionId,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails);

    @Operation(summary = "후기 숨기기", description = "내가 받은 후기 또는 보낸 후기를 내 목록에서 숨깁니다. 상대방 목록에는 영향을 주지 않고, 복구할 수 없습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "후기 숨김 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "REVIEW403-ACCESS_DENIED: 작성자 또는 받은 사용자가 아닙니다."),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "REVIEW404-NOT_FOUND: 후기를 찾을 수 없습니다.")
    })
    ApiResponse<Void> hide(@PathVariable Long reviewId, @AuthenticationPrincipal UserDetails userDetails);
}
