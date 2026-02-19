package com.fmi.domain.post.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.request.PostRadiusUpdateRequest;
import com.fmi.domain.post.web.dto.request.PostUpdateRequest;
import com.fmi.domain.post.web.dto.response.*;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.utils.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
@Tag(name = "Post", description = "게시글 관련 API")
public class PostController {

    private final PostService postService;
    private final PostQueryService postQueryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(@RequestPart("request") @Valid PostCreateRequest request,
                                                                      @RequestPart(value = "image", required = false) List<MultipartFile> images,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {

        PostCreateResponse response = postService.createPost(request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/search")
    @Operation(
            summary = "게시글 리스트 검색/필터/정렬 (무한스크롤)",
            description = """
                    주소 기반으로 게시글 목록을 조회합니다.
                    
                    - 무한스크롤: 첫 요청은 cursor 없이 호출하고, 이후 응답의 nextCursor를 cursor로 넣어 다음 페이지를 요청합니다.
                    - size: 한 번에 가져올 개수 (기본 20)
                    
                    예)
                    - 첫 요청: /posts/search?address=서울&sortType=LATEST&size=20
                    - 다음 요청: /posts/search?address=서울&sortType=LATEST&cursor=98&size=20
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    public ResponseEntity<ApiResponse<PostPageResponse>> searchPostFilterOrSort(@RequestParam(required = false) PostType postType,
                                                                                @RequestParam(required = false, defaultValue = "SEARCHING") PostStatus postStatus,
                                                                                @RequestParam(required = false) Category category,
                                                                                @RequestParam(required = false) String address,
                                                                                @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
                                                                                @AuthenticationPrincipal UserDetails userDetails,
                                                                                @RequestParam(required = false) Long cursor,
                                                                                @RequestParam(required = false, defaultValue = "20") int size) {
        PostPageResponse response = postQueryService.getPostListByFilterOrSort(postType, postStatus, category, address, sortType, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/search/keyword")
    @Operation(
            summary = "게시글 키워드 검색 (무한스크롤)",
            description = """
                    키워드로 게시글을 검색합니다. (title/content 기준)
                    
                    - 무한스크롤: 첫 요청은 cursor 없이 호출하고, 이후 응답의 nextCursor를 cursor로 넣어 다음 페이지를 요청합니다.
                    - size: 한 번에 가져올 개수 (기본 20)
                    
                    예)
                    - 첫 요청: /posts/search/keyword?keyword=지갑&size=20
                    - 다음 요청: /posts/search/keyword?keyword=지갑&cursor=98&size=20
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
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
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    public ResponseEntity<ApiResponse<PostPageResponse>> searchByKeyword(@RequestParam String keyword,
                                                                         @AuthenticationPrincipal UserDetails userDetails,
                                                                         @RequestParam(required = false) Long cursor,
                                                                         @RequestParam(required = false, defaultValue = "20") int size) {

        PostPageResponse postPageResponse = postQueryService.searchByKeyword(keyword, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(postPageResponse));
    }

    @GetMapping("/{postId}")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    게시글 상세 정보를 조회합니다.
                    
                    조회수 정책:
                    - 회원: userId 기준으로 하루 1회만 조회수 증가
                    - 비회원: IP(해시) 기준으로 하루 1회만 조회수 증가
                    - '하루'는 Asia/Seoul 기준 00:00 ~ 23:59 입니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다", content = @Content)
    })
    public ResponseEntity<ApiResponse<PostGetResponse>> getPost(@PathVariable Long postId,
                                                                @AuthenticationPrincipal UserDetails userDetails,
                                                                HttpServletRequest request) {

        String clientIp = (Objects.isNull(userDetails)) ? IpUtil.getClientIp(request) : null;

        PostGetResponse response = postQueryService.getPost(postId, userDetails, clientIp);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    public ResponseEntity<ApiResponse<PostUpdateResponse>> updatePost(@PathVariable Long postId,
                                                                      @RequestPart("request") PostUpdateRequest request,
                                                                      @RequestPart(value = "image", required = false) List<MultipartFile> images,
                                                                      @AuthenticationPrincipal UserDetails userDetails) {

        PostUpdateResponse response = postService.updatePost(postId, request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다")
    })
    public ResponseEntity<ApiResponse<String>> deletePost(@PathVariable Long postId,
                                                          @AuthenticationPrincipal UserDetails userDetails) {

        postService.deletePost(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("게시글 삭제 완료"));
    }


    @PutMapping("/{postId}/radius")
    @Operation(
            summary = "게시글 반경(radius) 수정",
            description = """
                    특정 게시글의 radius 값을 수정합니다.
                    
                    - 성공 시 204 No Content (응답 바디 없음)
                    - 권한이 없는 사용자는 403을 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "수정 성공 (응답 바디 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = @Content)
    })
    public ResponseEntity<Void> updateRadius(@PathVariable Long postId,
                                             @RequestBody PostRadiusUpdateRequest request,
                                             UserDetails userDetails) {

        postService.updateRadius(postId, request, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}/similar")
    @Operation(
            summary = "비슷한 분실물/습득물 추천 목록 조회",
            description = """
                    상세 조회 페이지 하단에 노출할 “비슷한 분실물/습득물” 추천 리스트를 조회합니다.
                    
                    기준
                    - 같은 카테고리 + 같은 지역(주소)
                    - 같은 Post type (분실/습득)
                    - 기준 게시글 날짜(createdAt) 기준 -7일 ~ +7일 범위
                    - 최신순(createdAt DESC) 최대 5개
                    - 로그인 시 favoriteStatus(내 즐겨찾기 여부) 포함
                    
                    예)
                    - /posts/10/similar
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
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
                                                  "createdAt": "2026-02-15T10:30:00"
                                                },
                                                {
                                                  "postId": 24,
                                                  "title": "카드 주웠어요",
                                                  "thumbnailImageUrl": null,
                                                  "address": "서울 강남구",
                                                  "favoriteCount": 2,
                                                  "favoriteStatus": false,
                                                  "viewCount": 33,
                                                  "createdAt": "2026-02-14T18:10:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<PostSimilarResponse>>> getSimilarList(@PathVariable Long postId,
                                                                                 @AuthenticationPrincipal UserDetails userDetails) {
        List<PostSimilarResponse> response = postQueryService.getSimilarPostList(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    //
//    @PostMapping("/draft")
//    @Operation(summary = "게시글 임시 저장")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 임시 저장 성공"),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
//    })
//    public ResponseEntity<ApiResponse<Void>> saveTemporaryPost(
//
//            @RequestPart("request") TemporaryPostDto request,
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestPart(value = "images", required = false) List<MultipartFile> images
//    ) {
//        postService.saveTemporaryPost(request, userDetails, images);
//        return ResponseEntity.ok(ApiResponse.onSuccess(null));
//    }
//
//    @GetMapping("/draft")
//    @Operation(summary = "임시 저장 조회")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 저장 조회 성공")
//    })
//    public ResponseEntity<ApiResponse<PostResponse>> getTemporaryPost(
//            @AuthenticationPrincipal UserDetails userDetails
//    ) {
//        PostResponse post = postService.getTemporaryPost(userDetails);
//        return ResponseEntity.ok(ApiResponse.onSuccess(post));
//    }
//
//    @DeleteMapping("/draft")
//    @Operation(summary = "임시 저장 삭제")
//    @ApiResponses({
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 저장 삭제 성공")
//    })
//    public ResponseEntity<ApiResponse<String>> deleteTemporaryPost(
//            @AuthenticationPrincipal UserDetails userDetails
//    ) {
//        postService.deleteTemporaryPost(userDetails);
//
//        return ResponseEntity.ok(ApiResponse.onSuccess("임시 게시글 삭제 완료"));
//    }
//
    @GetMapping("/{postId}/share")
    @Operation(summary = "메타 데이터용 api")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 공유 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다")
    })
    public ResponseEntity<ApiResponse<PostShareResponse>> sharePost(@PathVariable Long postId) {

        PostShareResponse shareResponse = postQueryService.getSharePost(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(shareResponse));
    }

}
