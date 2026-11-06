package com.fmi.domain.admin.web.controller;

import com.fmi.domain.notice.notice.service.NoticeService;
import com.fmi.domain.notice.notice.web.dto.request.NoticeCreateRequest;
import com.fmi.domain.notice.notice.web.dto.request.NoticeUpdateRequest;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping("/notices")
    @Operation(summary = "공지 생성(관리자)")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 생성 성공")
    })
    public ApiResponse<Long> createNotice(
            @Valid @RequestBody NoticeCreateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        Long id = noticeService.createNotice(request, userDetails.getUsername());
        return ApiResponse.onSuccess(id);
    }

    @GetMapping("/notices/draft")
    @Operation(summary = "임시저장 공지사항 조회(관리자)", description = "현재 로그인한 관리자의 임시저장 공지사항을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시저장 조회 성공")
    })
    public ApiResponse<NoticeResponse> getDraftNotice(@AuthenticationPrincipal UserDetails userDetails) {
        NoticeResponse response = noticeService.getDraftNotice(userDetails.getUsername());
        return ApiResponse.onSuccess(response);
    }

    @PutMapping("/notices/{noticeId}")
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
    public ApiResponse<NoticeResponse> updateNotice(
            @PathVariable Long noticeId, @Valid @RequestBody NoticeUpdateRequest request) {
        NoticeResponse response = noticeService.updateNotice(noticeId, request);
        return ApiResponse.onSuccess(response);
    }

    @DeleteMapping("/notices/{noticeId}")
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
    public ApiResponse<String> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess("공지사항 삭제 완료");
    }
}
