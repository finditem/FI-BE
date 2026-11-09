package com.fmi.domain.admin.web.swagger;

import com.fmi.domain.notice.notice.web.dto.request.NoticeCreateRequest;
import com.fmi.domain.notice.notice.web.dto.request.NoticeUpdateRequest;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Admin", description = "관리자 전용 API")
public interface AdminNoticeSwagger {

    @Operation(summary = "공지 생성(관리자)")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 생성 성공")
    })
    ApiResponse<Long> createNotice(NoticeCreateRequest request, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "임시저장 공지사항 조회(관리자)", description = "현재 로그인한 관리자의 임시저장 공지사항을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 조회 성공")
    })
    ApiResponse<NoticeResponse> getDraftNotice(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "공지 수정(관리자)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "공지 수정 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": {\"noticeId\": 1, \"title\": \"수정 제목\", \"content\": \"수정 내용\", \"category\": \"GENERAL\", \"pinned\": false, \"viewCount\": 10, \"likeCount\": 12, \"commentCount\": 5, \"authorName\": \"관리자\", \"thumbnailUrl\": null, \"images\": [], \"isNew\": false, \"isHot\": false, \"likeStatus\": false, \"createdAt\": \"2024-01-01T00:00:00\", \"updatedAt\": \"2024-01-02T00:00:00\"}}"))),
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
    ApiResponse<NoticeResponse> updateNotice(Long noticeId, NoticeUpdateRequest request);

    @Operation(summary = "공지 삭제(관리자)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "공지 삭제 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": true, \"code\": \"COMMON200\", \"message\": \"성공\", \"result\": \"공지사항 삭제 완료\"}"))),
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
    ApiResponse<String> deleteNotice(Long noticeId);
}
