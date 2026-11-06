package com.fmi.domain.admin.web.controller;

import com.fmi.domain.admin.service.AdminPostQueryService;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.enums.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API")
public class AdminPostController {

    private final AdminPostQueryService adminPostQueryService;

    @GetMapping("/posts/content-policy")
    @Operation(summary = "콘텐츠 활용 동의 유저 게시글 목록 조회", description = """
                콘텐츠 활용에 동의한 유저들의 게시글을 조회합니다.
                커서 기반 무한스크롤을 지원하며, 정렬/카테고리/찾음 여부/키워드 필터를 제공합니다.

                **정렬 (sortType):**
                - LATEST: 최신순 (기본값)
                - OLDEST: 오래된순
                - MOST_VIEWED: 조회수 많은순
                - MOST_FAVORITED: 즐겨찾기 많은순

                **필터:**
                - category: 카테고리 필터 (ELECTRONICS, WALLET, ID_CARD, JEWELRY, BAG, CARD, ETC)
                - postStatus: 찾음 여부 필터 (SEARCHING, FOUND)
                - keyword: 제목/내용 검색
                - startDate: 조회 시작일 (yyyy-MM-dd)
                - endDate: 조회 종료일 (yyyy-MM-dd)

                **커서:**
                - cursor: 마지막 게시글 ID
                - cursorViewCount: MOST_VIEWED 정렬 시 마지막 게시글의 조회수
                - cursorFavCount: MOST_FAVORITED 정렬 시 마지막 게시글의 즐겨찾기 수
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자만 접근할 수 있습니다")
    })
    public ApiResponse<CursorPageResponse<PostBriefResponse>> getContentPolicyPosts(
            @Parameter(description = "정렬 (LATEST=최신순, OLDEST=오래된순, MOST_VIEWED=조회수순, MOST_FAVORITED=즐겨찾기순)")
                    @RequestParam(defaultValue = "LATEST")
                    SortType sortType,
            @Parameter(description = "카테고리 필터") @RequestParam(required = false) Category category,
            @Parameter(description = "찾음 여부 필터 (SEARCHING=찾는중, FOUND=찾음)") @RequestParam(required = false)
                    PostStatus postStatus,
            @Parameter(description = "제목/내용 키워드 검색") @RequestParam(required = false) String keyword,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-01-01")
                    @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-12-31")
                    @RequestParam(required = false)
                    @org.springframework.format.annotation.DateTimeFormat(
                            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                    java.time.LocalDate endDate,
            @Parameter(description = "커서 (마지막 게시글 ID)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "MOST_VIEWED 정렬 시 마지막 게시글의 조회수") @RequestParam(required = false)
                    Long cursorViewCount,
            @Parameter(description = "MOST_FAVORITED 정렬 시 마지막 게시글의 즐겨찾기 수") @RequestParam(required = false)
                    Long cursorFavCount,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        CursorPageResponse<PostBriefResponse> response = adminPostQueryService.getContentPolicyPosts(
                sortType,
                category,
                postStatus,
                keyword,
                startDate,
                endDate,
                cursor,
                cursorViewCount,
                cursorFavCount,
                size,
                userDetails.getUsername());
        return ApiResponse.onSuccess(response);
    }
}
