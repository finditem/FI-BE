package com.fmi.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "운영진 게스트 문의 목록 응답 (커서 방식)")
public record AdminGuestInquiryPageResponse(
        @Schema(description = "목록") List<AdminInquiryResponse> items,

        @Schema(description = "다음 페이지 커서 (마지막 문의 id, 다음 요청 시 cursor 파라미터로 전달)")
        Long nextCursor,

        @Schema(description = "다음 페이지 존재 여부") boolean hasNext) {}
