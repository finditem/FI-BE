package com.fmi.domain.post.post.web.swagger;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.post.web.dto.request.PostRadiusUpdateRequest;
import com.fmi.domain.post.post.web.dto.request.PostStatusUpdateRequest;
import com.fmi.domain.post.post.web.dto.request.PostUpdateRequest;
import com.fmi.domain.post.post.web.dto.response.PostCreateResponse;
import com.fmi.domain.post.post.web.dto.response.PostGetResponse;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.post.post.web.dto.response.PostShareResponse;
import com.fmi.domain.post.post.web.dto.response.PostSimilarResponse;
import com.fmi.domain.post.post.web.dto.response.PostUpdateResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Post", description = "게시글 관련 API")
public interface PostSwagger {

    @Operation(summary = "게시글 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "게시글 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "415",
                description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            PostCreateRequest request, List<MultipartFile> images, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "게시글 리스트 검색/필터/정렬 (무한스크롤)", description = """
                    주소 기반으로 게시글 목록을 조회합니다. 선택 인증이며, 로그인하면 사용자별 정보를 반영합니다.

                    - 무한스크롤: 첫 요청은 cursor 없이 호출하고, 이후 응답의 nextCursor를 cursor로 넣어 다음 페이지를 요청합니다.
                    - size: 한 번에 가져올 개수 (기본 20)

                    예)
                    - 첫 요청: /posts/search?address=서울&sortType=LATEST&size=20
                    - 다음 요청: /posts/search?address=서울&sortType=LATEST&cursor=98&size=20
                    """)
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = @Content)
    })
    ResponseEntity<ApiResponse<PostPageResponse>> searchPostFilterOrSort(
            PostType postType,
            PostStatus postStatus,
            Category category,
            String address,
            SortType sortType,
            @Parameter(hidden = true) UserDetails userDetails,
            Long cursor,
            int size);

    @Operation(summary = "게시글 키워드 검색 (무한스크롤)", description = """
                    키워드로 게시글을 검색합니다. (title/content 기준)

                    - 무한스크롤: 첫 요청은 cursor 없이 호출하고, 이후 응답의 nextCursor를 cursor로 넣어 다음 페이지를 요청합니다.
                    - size: 한 번에 가져올 개수 (기본 20)

                    예)
                    - 첫 요청: /posts/search/keyword?keyword=지갑&size=20
                    - 다음 요청: /posts/search/keyword?keyword=지갑&cursor=98&size=20
                    """)
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "조회 성공",
                content = @Content(examples = @ExampleObject(value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": {
                                                "postList": [
                                                  {
                                                    "id": 101,
                                                    "title": "지갑 분실했어요",
                                                    "summary": "어제 강남역 근처에서...",
                                                    "thumbnailUrl": "https://example.com/thumb.png",
                                                    "address": "서울 강남구",
                                                    "postStatus": "SEARCHING",
                                                    "postType": "LOST",
                                                    "category": "WALLET",
                                                    "favoriteCount": 3,
                                                    "isFavorite": false,
                                                    "viewCount": 12,
                                                    "isNew": true,
                                                    "isHot": false,
                                                    "createdAt": "2024-01-01T00:00:00",
                                                    "imageCount": 1
                                                  }
                                                ],
                                                "postCount": 42,
                                                "nextCursor": 101,
                                                "hasNext": true
                                              }
                                            }
                                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = @Content)
    })
    ResponseEntity<ApiResponse<PostPageResponse>> searchByKeyword(
            String keyword, @Parameter(hidden = true) UserDetails userDetails, Long cursor, int size);

    @Operation(summary = "게시글 상세 조회", description = """
                    게시글 상세 정보를 조회합니다.

                    조회수 정책:
                    - 회원: userId 기준으로 하루 1회만 조회수 증가
                    - 비회원: IP(해시) 기준으로 하루 1회만 조회수 증가
                    - '하루'는 Asia/Seoul 기준 00:00 ~ 23:59 입니다.
                    """)
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "게시글 상세 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다",
                content = @Content)
    })
    ResponseEntity<ApiResponse<PostGetResponse>> getPost(
            Long postId, @Parameter(hidden = true) UserDetails userDetails, HttpServletRequest request);

    @Operation(
            summary = "게시글 수정",
            description = "작성자만 수정할 수 있습니다. 한 게시글은 1분에 5번까지 수정할 수 있고, 6번째 요청부터는 429 응답으로 차단되며 3분 뒤에 다시 수정할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "게시글 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "POST403-ACCESS_DENIED: 해당 글에 접근 권한이 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "415",
                description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "429",
                description = "POST429-UPDATE_RATE_LIMITED: 게시글 수정이 1분에 5번을 초과했습니다. "
                        + "result.retryAfterSeconds(재시도까지 남은 초)만큼 기다린 뒤 다시 요청해야 합니다",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": false,
                                      "code": "POST429-UPDATE_RATE_LIMITED",
                                      "message": "게시글 수정이 1분에 5번을 초과했습니다. 잠시 후 다시 시도해주세요.",
                                      "result": {
                                        "retryAfterSeconds": 180
                                      }
                                    }
                                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    ResponseEntity<ApiResponse<PostUpdateResponse>> updatePost(
            Long postId,
            PostUpdateRequest request,
            List<MultipartFile> images,
            @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "게시글 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다")
    })
    ResponseEntity<ApiResponse<String>> deletePost(Long postId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "게시글 반경(radius) 수정", description = """
                    특정 게시글의 radius 값을 수정합니다.

                    - 성공 시 204 No Content (응답 바디 없음)
                    - 권한이 없는 사용자는 403을 반환합니다.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "수정 성공 (응답 바디 없음)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "요청 값 검증 실패",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "수정 권한 없음",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글을 찾을 수 없음",
                content = @Content)
    })
    ResponseEntity<Void> updateRadius(Long postId, PostRadiusUpdateRequest request, UserDetails userDetails);

    @Operation(summary = "비슷한 분실물/습득물 추천 목록 조회", description = """
                    상세 조회 페이지 하단에 노출할 “비슷한 분실물/습득물” 추천 리스트를 조회합니다.

                    기준
                    - 같은 카테고리 + 같은 지역(주소)
                    - 같은 Post type (분실/습득)
                    - 기준 게시글 날짜(createdAt) 기준 -7일 ~ +7일 범위
                    - 최신순(createdAt DESC) 최대 5개
                    - 로그인 시 favoriteStatus(내 즐겨찾기 여부) 포함

                    예)
                    - /posts/10/similar
                    """)
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "조회 성공",
                content = @Content(examples = @ExampleObject(value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": [
                                                {
                                                  "postId": 25,
                                                  "title": "지갑 습득했습니다",
                                                  "thumbnailImageUrl": "https://example.com/thumb.jpg",
                                                  "address": "서울 강남구",
                                                  "favoriteCount": 12,
                                                  "favoriteStatus": true,
                                                  "viewCount": 104,
                                                  "createdAt": "2026-02-15T10:30:00",
                                                  "category": "ELECTRONICS"
                                                },
                                                {
                                                  "postId": 24,
                                                  "title": "카드 주웠어요",
                                                  "thumbnailImageUrl": null,
                                                  "address": "서울 강남구",
                                                  "favoriteCount": 2,
                                                  "favoriteStatus": false,
                                                  "viewCount": 33,
                                                  "createdAt": "2026-02-14T18:10:00",
                                                  "category": "ELECTRONICS"
                                                }
                                              ]
                                            }
                                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글을 찾을 수 없음",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
                content = @Content)
    })
    ResponseEntity<ApiResponse<List<PostSimilarResponse>>> getSimilarList(
            Long postId, @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "메타 데이터용 api")
    @SecurityRequirements
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                useReturnTypeSchema = true,
                description = "게시글 공유 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다",
                content = @Content)
    })
    ResponseEntity<ApiResponse<PostShareResponse>> sharePost(Long postId);

    @Operation(summary = "게시글 상태 변경", description = """
                    게시글의 상태를 변경합니다.

                    - 본인 게시글만 변경 가능
                    - 인증 필요 (JWT)
                    - 요청 Body에 변경할 상태 값을 전달

                    예)
                    PUT /posts/{postId}/status

                    요청 예시:
                    {
                      "status": "FOUND"
                    }
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": null
                                            }
                                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 상태 값 전달",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음 (본인 게시글 아님)",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글을 찾을 수 없음",
                content = @Content)
    })
    ResponseEntity<ApiResponse<Void>> updateFound(
            Long postId, PostStatusUpdateRequest request, @Parameter(hidden = true) UserDetails userDetails);
}
