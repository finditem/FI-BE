package com.fmi.domain.inquiry.inquiry.web.swagger;

import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryListResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Inquiry", description = "문의 API")
public interface InquirySwagger {

    @Operation(summary = "1:1 개인 문의 작성", description = "비회원도 가능하며, 비회원인 경우 email 필수입니다. 이미지 첨부 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "INQUIRY400-GUEST_EMAIL_REQUIRED: 비회원 문의는 이메일이 필수입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY400-GUEST_EMAIL_REQUIRED\", \"message\": \"비회원 문의는 이메일이 필수입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "INQUIRY403-IP_BLOCKED: 차단된 IP입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY403-IP_BLOCKED\", \"message\": \"차단된 IP입니다.\"}")))
    })
    ApiResponse<Long> createInquiry(
            com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryCreateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails,
            HttpServletRequest httpServletRequest);

    @Operation(summary = "내 문의 내역 조회", description = "커서 기반 페이지네이션으로 본인의 1:1 개인 문의를 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 문의 내역 조회 성공")
    })
    ApiResponse<CursorPageResponse<InquiryListResponse>> getMyInquiries(
            @Parameter(hidden = true) UserDetails userDetails,
            @Parameter(description = "문의 상태 필터 (RECEIVED=접수 대기, PENDING=처리 중, ANSWERED=답변 완료)") InquiryStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)") Boolean answered,
            @Parameter(description = "검색 키워드 (제목, 내용)") String keyword,
            Long cursor,
            int size);

    @Operation(summary = "문의 상세 조회", description = "본인의 1:1 개인 문의만 조회 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "INQUIRY403-ACCESS_DENIED: 해당 문의를 조회할 권한이 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY403-ACCESS_DENIED\", \"message\": \"해당 문의를 조회할 권한이 없습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}")))
    })
    ApiResponse<InquiryDetailResponse> getInquiryDetail(
            Long inquiryId, @Parameter(hidden = true) UserDetails userDetails);
}
