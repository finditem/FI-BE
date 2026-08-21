package com.fmi.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "관리자 비회원 문의 목록 응답 (커서 기반 무한스크롤)")
public record AdminGuestInquiryPageResponse(
        @Schema(description = "목록") List<AdminInquiryResponse> items,

        @Schema(description = "다음 페이지 커서 (마지막 문의 id, 다음 요청 시 cursor 파라미터로 전달)")
        Long nextCursor,

        @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {}
