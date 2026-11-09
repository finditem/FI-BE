package com.fmi.domain.post.post.web.swagger;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.response.PostFavoriteResponse;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Post", description = "게시글 관련 API")
public interface PostFavoriteSwagger {

    @Operation(
            summary = "즐겨찾기 추가",
            description = "즐겨찾기를 추가합니다.",
            tags = {"Post"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다")
    })
    ResponseEntity<ApiResponse<PostFavoriteResponse>> createFavorite(
            Long postId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(
            summary = "즐겨찾기 삭제",
            description = "즐겨찾기를 삭제합니다.",
            tags = {"Post"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = """
                        - POST404-NOT_FOUND: 존재하지 않는 게시글입니다
                        - POST-FAVORITE-NOT_FOUND: 해당 게시글에 대한 즐겨찾기를 하지 않았습니다.
                        """)
    })
    ResponseEntity<ApiResponse<PostFavoriteResponse>> deleteFavorite(
            Long postId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(
            summary = "즐겨찾기 목록 조회",
            description = """
            현재 로그인한 사용자의 즐겨찾기 목록을 커서 기반으로 조회합니다.

            **필터 파라미터** (모두 선택):
            - postType: LOST / FOUND
            - category: ELECTRONICS / WALLET / ID_CARD / JEWELRY / BAG / CARD / ETC
            - address: 지역 필터 (예: 서울특별시, 서울특별시 강남구)
            - keyword: 제목 또는 내용 검색
            """,
            tags = {"User"})
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공")
    })
    ResponseEntity<ApiResponse<PostPageResponse>> getFavoritePost(
            @Parameter(hidden = true) UserDetails userDetails,
            PostType postType,
            Category category,
            String address,
            String keyword,
            SortType sortType,
            Long cursor,
            int size);
}
