package com.fmi.domain.admin.web.swagger;

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
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Admin", description = "관리자 전용 API")
public interface AdminPostSwagger {

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
    ApiResponse<CursorPageResponse<PostBriefResponse>> getContentPolicyPosts(
            @Parameter(description = "정렬 (LATEST=최신순, OLDEST=오래된순, MOST_VIEWED=조회수순, MOST_FAVORITED=즐겨찾기순)")
                    SortType sortType,
            @Parameter(description = "카테고리 필터") Category category,
            @Parameter(description = "찾음 여부 필터 (SEARCHING=찾는중, FOUND=찾음)") PostStatus postStatus,
            @Parameter(description = "제목/내용 키워드 검색") String keyword,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd)", example = "2026-01-01") java.time.LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd)", example = "2026-12-31") java.time.LocalDate endDate,
            @Parameter(description = "커서 (마지막 게시글 ID)") Long cursor,
            @Parameter(description = "MOST_VIEWED 정렬 시 마지막 게시글의 조회수") Long cursorViewCount,
            @Parameter(description = "MOST_FAVORITED 정렬 시 마지막 게시글의 즐겨찾기 수") Long cursorFavCount,
            int size,
            @Parameter(hidden = true) UserDetails userDetails);
}
