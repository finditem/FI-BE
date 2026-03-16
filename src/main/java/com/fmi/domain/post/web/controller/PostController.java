package com.fmi.domain.post.web.controller;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.service.PostQueryService;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.service.TemporaryPostService;
import com.fmi.domain.post.web.dto.request.*;
import com.fmi.domain.post.web.dto.response.*;
import com.fmi.domain.post.web.dto.response.temp.TemporaryPostResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.utils.IpUtil;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
    private final TemporaryPostService temporaryPostService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다")
    })
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestPart("request") @Valid PostCreateRequest request,
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostPageResponse.class)
                    )
            ),
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
                            schema = @Schema(implementation = PostPageResponse.class),
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostGetResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다", content = @Content)
    })
    public ResponseEntity<ApiResponse<PostGetResponse>> getPost(@PathVariable Long postId,
                                                                @AuthenticationPrincipal UserDetails userDetails,
                                                                HttpServletRequest request) {

        String clientIp = (Objects.isNull(userDetails)) ? IpUtil.getClientIp(request) : null;

        PostGetResponse response = postQueryService.getPost(postId, userDetails, clientIp);

        // 게시글 조회 시 관련 알림 자동 읽음처리
        if (userDetails != null) {
            userRepository.findByEmail(userDetails.getUsername()).ifPresent(user ->
                    notificationService.markNotificationsAsReadByReference(user, postId,
                            List.of(NotificationType.COMMENT, NotificationType.REPLY, NotificationType.FAVORITE, NotificationType.CATEGORY))
            );
        }

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostUpdateResponse.class)
                    )
            ),
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
                            array = @ArraySchema(
                                    schema = @Schema(implementation = PostSimilarResponse.class)
                            ),
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

    @PostMapping(value = "/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "임시 저장 게시글 생성/수정",
            description = """
                    임시 저장 게시글을 생성하거나(없으면 생성) 기존 임시 저장 게시글을 업데이트합니다.
                    
                    - 회원당 임시 저장 게시글은 1개만 존재합니다.
                    - request 파트(JSON)로 임시 저장 데이터를 전달합니다. (필드는 모두 nullable)
                    - images 파트(파일)는 선택이며, 신규 업로드 이미지입니다.
                    - keepImageIdList: 기존 임시 저장에 이미 저장되어 있던 이미지 중 '유지할 이미지 ID' 목록입니다.
                      - 기존 임시 저장이 있는 경우에만 적용됩니다.
                      - keepImageIdList에 포함되지 않은 기존 이미지는 S3/DB에서 삭제됩니다.
                    
                    요청 예)
                    - 신규 임시저장 생성: request만 전송 또는 request + images 전송
                    - 임시저장 수정(기존 이미지 유지 + 새 이미지 추가):
                      - request.keepImageIdList=[1,2]
                      - images에 신규 파일 추가
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "임시 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공",
                                      "result": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (multipart 형식 오류/파라미터 오류)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> saveTemporaryPost(@RequestPart("request") TemporaryPostCreateRequest request,
                                                               @AuthenticationPrincipal UserDetails userDetails,
                                                               @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        temporaryPostService.create(request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }


    @GetMapping("/temp")
    @Operation(
            summary = "내 임시 저장 게시글 조회",
            description = """
                    로그인한 사용자의 임시 저장 게시글(temporarySave=true)을 조회합니다.
                    
                    - 임시 저장 게시글이 없으면 result는 null로 반환됩니다.
                    - images에는 임시 저장 게시글에 연결된 이미지 목록이 포함됩니다. (id/imgUrl/imageType)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "임시 저장 존재",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "성공",
                                                      "result": {
                                                        "postId": 24,
                                                        "postType": "LOST",
                                                        "category": "WALLET",
                                                        "radius": "RADIUS_1000",
                                                        "date": "2026-02-20T10:30:00",
                                                        "title": "지갑을 잃어버렸어요",
                                                        "address": "서울 강남구",
                                                        "latitude": 37.4979,
                                                        "longitude": 127.0276,
                                                        "content": "강남역 근처에서 검정 지갑 분실했습니다.",
                                                        "images": [
                                                          {
                                                            "id": 12,
                                                            "imgUrl": "https://example.com/image1.png",
                                                            "imageType": "THUMBNAIL"
                                                          },
                                                          {
                                                            "id": 13,
                                                            "imgUrl": "https://example.com/image2.png",
                                                            "imageType": "NORMAL"
                                                          }
                                                        ],
                                                        "updatedAt": "2026-02-20T12:00:00",
                                                        "createdAt": "2026-02-20T10:00:00"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "임시 저장 없음",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "성공",
                                                      "result": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> getTemporaryPost(@AuthenticationPrincipal UserDetails userDetails) {
        TemporaryPostResponse response = temporaryPostService.getMyTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/temp")
    @Operation(
            summary = "내 임시 저장 게시글 삭제",
            description = """
                    로그인한 사용자의 임시 저장 게시글(temporarySave=true)을 삭제합니다.
                    
                    - 임시 저장 게시글이 존재하면: 연결된 이미지(S3 + DB) 삭제 후 게시글을 삭제합니다.
                    - 임시 저장 게시글이 없으면: _TEMP_POST_NOT_FOUND 예외를 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteTemporaryPost(@AuthenticationPrincipal UserDetails userDetails) {
        temporaryPostService.deleteTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }


    @GetMapping("/{postId}/share")
    @Operation(summary = "메타 데이터용 api")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 공유 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostShareResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<PostShareResponse>> sharePost(@PathVariable Long postId) {

        PostShareResponse shareResponse = postQueryService.getSharePost(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(shareResponse));
    }

    @PutMapping("/{postId}/status")
    @Operation(
            summary = "게시글 상태 변경",
            description = """
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
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상태 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 값 전달", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (본인 게시글 아님)", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> updateFound(@PathVariable Long postId,
                                                         @RequestBody @Valid PostStatusUpdateRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        postService.updatePostStatus(postId, request, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

}
