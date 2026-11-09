package com.fmi.domain.notice.notice.web.swagger;

import com.fmi.domain.notice.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.notice.data.enums.NoticeSortType;
import com.fmi.domain.notice.notice.web.dto.response.NoticeListResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeMetaResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Notice", description = "공지사항 API")
public interface NoticeSwagger {

    @Operation(
            summary = "공지사항 목록 조회",
            description = "커서 기반 페이지네이션으로 공지사항을 조회합니다. cursor를 생략하면 최신 항목부터 반환합니다. 상단 고정 공지사항이 먼저 표시됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 목록 조회 성공")
    })
    ApiResponse<CursorPageResponse<NoticeListResponse>> getNoticeList(
            NoticeCategory category,
            String keyword,
            @Parameter(description = "정렬 기준 (LATEST=최신순, OLDEST=오래된순, MOST_VIEWED=조회많은순)") NoticeSortType sortType,
            Long cursor,
            int size);

    @Operation(summary = "공지사항 메타데이터 조회", description = "OG 태그 생성용 경량 API. 제목과 본문 요약(100자)을 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메타데이터 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "NOTICE404-NOT_FOUND: 존재하지 않는 공지사항입니다")
    })
    ApiResponse<NoticeMetaResponse> getNoticeMeta(Long noticeId);

    @Operation(summary = "공지사항 상세 조회", description = "조회수가 5분마다만 증가합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지사항 상세 조회 성공"),
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
    ApiResponse<NoticeResponse> getNoticeDetail(Long noticeId, HttpServletRequest request);
}
