package com.fmi.domain.admin.web.swagger;

import com.fmi.domain.admin.web.dto.request.AdminGuestInquiryReplyRequest;
import com.fmi.domain.admin.web.dto.request.AdminIpBlockRequest;
import com.fmi.domain.admin.web.dto.response.AdminGuestInquiryPageResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryDetailResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryResponse;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Admin", description = "관리자 전용 API")
public interface AdminInquirySwagger {

    @Operation(summary = "관리자 비회원 문의 상세 조회", description = "비회원이 작성한 문의의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비회원 문의 상세 조회 성공"),
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
    ApiResponse<InquiryDetailResponse> getGuestInquiryDetail(Long inquiryId);

    @Operation(summary = "관리자 비회원 문의 목록 조회", description = """
            비회원이 작성한 문의 내역을 조회합니다. (커서 기반 무한스크롤)

            - 첫 요청: cursor 없이 호출
            - 다음 요청: 응답의 nextCursor를 cursor 파라미터로 전달
            예) /admin/guest-inquiries?size=20 → /admin/guest-inquiries?cursor=98&size=20
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비회원 문의 목록 조회 성공")
    })
    ApiResponse<AdminGuestInquiryPageResponse> getGuestInquiries(
            InquiryStatus status, Boolean answered, String keyword, Long cursor, int size);

    @Operation(
            summary = "관리자 회원 문의 목록 조회",
            description = "커서 기반 페이지네이션으로 회원이 작성한 문의 내역을 조회합니다. (비회원 문의 제외) cursor를 생략하면 최신 항목부터 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 목록 조회 성공")
    })
    ApiResponse<CursorPageResponse<AdminInquiryResponse>> getInquiries(
            InquiryType type,
            InquiryStatus status,
            @Parameter(description = "답변 여부 필터 (true=답변완료, false=미답변)") Boolean answered,
            String keyword,
            Long cursor,
            int size);

    @Operation(summary = "관리자 문의 상세 조회", description = "문의 상세 정보를 조회합니다. 관리자는 비공개 문의도 조회 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 상세 조회 성공"),
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
    ApiResponse<AdminInquiryDetailResponse> getInquiryDetail(
            Long inquiryId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "비회원 문의 답변 (이메일 발송)", description = "비회원 문의에 대해 이메일로 답변을 발송합니다. 상태가 ANSWERED로 변경됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 발송 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "회원 문의이거나 이메일이 없는 경우",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"COMMON400\", \"message\": \"잘못된 요청입니다.\"}")))
    })
    ApiResponse<String> replyToGuestInquiry(Long inquiryId, AdminGuestInquiryReplyRequest request);

    @Operation(summary = "문의 처리 상태 변경(관리자)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 처리 상태 변경 성공"),
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
    ApiResponse<String> updateInquiryStatus(
            Long inquiryId, com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryStatusUpdateRequest request);

    @Operation(summary = "문의 IP 차단(관리자)", description = "문의에 저장된 IP를 블랙리스트에 등록합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IP 차단 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "INQUIRY404-NOT_FOUND: 존재하지 않는 문의입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY404-NOT_FOUND\", \"message\": \"존재하지 않는 문의입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "INQUIRY400-IP_NOT_FOUND: 문의에 IP 정보가 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY400-IP_NOT_FOUND\", \"message\": \"문의에 IP 정보가 없습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "INQUIRY409-IP_ALREADY_BLOCKED: 이미 차단된 IP입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"INQUIRY409-IP_ALREADY_BLOCKED\", \"message\": \"이미 차단된 IP입니다.\"}")))
    })
    ApiResponse<String> blockInquiryIp(Long inquiryId, AdminIpBlockRequest request);
}
