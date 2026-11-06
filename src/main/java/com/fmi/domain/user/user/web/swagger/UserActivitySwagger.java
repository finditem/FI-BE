package com.fmi.domain.user.user.web.swagger;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.user.user.data.enums.ActivityType;
import com.fmi.domain.user.user.web.dto.response.MyActivityPageResponse;
import com.fmi.domain.user.user.web.dto.response.MyCommentPageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 정보 관리 API")
public interface UserActivitySwagger {

    @Operation(summary = "내가 쓴 댓글 목록", description = """
                현재 로그인한 사용자가 작성한 댓글 목록을 커서 기반으로 조회합니다. 삭제된 댓글은 제외됩니다.

                **필터 파라미터** (모두 선택):
                - startDate, endDate: yyyy-MM-dd 형식 (예: 2024-01-01)
                - keyword: 댓글 내용 또는 게시글 제목 검색
                - sort: LATEST(최신순, 기본값) / OLDEST(오래된순)

                **페이지네이션:**
                - cursor: 다음 페이지 시작점 (이전 응답의 nextCursor 값)
                - size: 한 페이지당 항목 수 (기본값 20)
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 댓글 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    ApiResponse<MyCommentPageResponse> getMyComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sort,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "내가 쓴 게시글 목록", description = """
                현재 로그인한 사용자가 작성한 게시글 목록을 커서 기반으로 조회합니다.

                **필터 파라미터** (모두 선택):
                - postType: LOST / FOUND
                - postStatus: SEARCHING / FOUND
                - category: ELECTRONICS / WALLET / ID_CARD / JEWELRY / BAG / CARD / ETC
                - startDate, endDate: yyyy-MM-dd 형식 (예: 2024-01-01)
                - keyword: 제목 또는 내용 검색
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 게시글 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    ApiResponse<PostPageResponse> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size);

    @Operation(summary = "내 활동 내역 통합 조회", description = """
                현재 로그인한 사용자의 모든 활동 내역을 통합하여 최신순으로 조회합니다.
                응답은 날짜별로 그룹화되어 반환됩니다.

                **활동 유형 (type):**
                - POST: 작성한 게시글
                - COMMENT: 작성한 댓글
                - FAVORITE: 즐겨찾기한 게시글
                - INQUIRY: 문의 내역 (전체)
                - INQUIRY_RECEIVED: 접수된 문의 (미답변)
                - INQUIRY_ANSWERED: 답변 완료된 문의
                - REPORT: 신고 내역 (전체)
                - REPORT_RECEIVED: 접수된 신고 (미답변)
                - REPORT_ANSWERED: 답변 완료된 신고
                - 미지정 시 전체 활동 조회

                **날짜 필터:**
                - startDate, endDate: yyyy-MM-dd 형식 (예: 2024-01-01)

                **검색:**
                - keyword: 제목 또는 내용 검색

                **커서**: ISO datetime 형식 (예: 2024-01-01T00:00:00)
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 내역 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    ApiResponse<MyActivityPageResponse> getMyActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size);
}
