package com.fmi.domain.map.web.controller;

import com.fmi.domain.map.service.PostMapService;
import com.fmi.domain.map.web.dto.request.MapPostRequest;
import com.fmi.domain.map.web.dto.request.PostMarkerRequest;
import com.fmi.domain.map.web.dto.request.RecentFoundPostRequest;
import com.fmi.domain.map.web.dto.response.MapPostResponse;
import com.fmi.domain.map.web.dto.response.PostMarkerResponse;
import com.fmi.domain.map.web.dto.response.RecentFoundPostResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main/posts")
public class MapController {
    private final PostMapService postMapService;

    @GetMapping("/marker")
    @Operation(
            summary = "지도 마커 조회",
            description = """
                    현재 지도 중심 좌표와 level(줌 레벨)을 기준으로 반경 내 게시글 마커 정보를 조회합니다.
                    
                    - 지도에 표시할 최소 정보만 반환합니다.
                    - 인증 없이도 조회할 수 있습니다.
                    - 로그인 사용자인 경우 차단 유저 게시글은 제외될 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지도 마커 조회 성공",
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
                                                  "postId": 1,
                                                  "latitude": 37.5665,
                                                  "longitude": 126.9780,
                                                  "category": "WALLET",
                                                  "postType": "FOUND",
                                                  "postStatus": "SEARCHING"
                                                },
                                                {
                                                  "postId": 2,
                                                  "latitude": 37.5651,
                                                  "longitude": 126.9895,
                                                  "category": "ELECTRONICS",
                                                  "postType": "LOST",
                                                  "postStatus": "SEARCHING"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 에러",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<List<PostMarkerResponse>>> getPostsForMap(@Valid @ModelAttribute PostMarkerRequest request,
                                                                                @AuthenticationPrincipal UserDetails userDetails) {
        List<PostMarkerResponse> response = postMapService.getPostMaker(request, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/recent-found")
    @Operation(
            summary = "최근 습득된 분실물 조회",
            description = """
                    지도 중심 좌표와 level을 기준으로 반경 내에서 
                    최근 습득된 분실물 게시글 10개를 조회합니다.
                    
                    - 지도 범위내 게시글만 조회됩니다.
                    - 최신순으로 정렬됩니다.
                    - 인증 없이 조회 가능합니다.
                    - 로그인 사용자인 경우 차단된 유저의 게시글은 제외됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최근 습득 게시글 조회 성공",
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
                                                  "postId": 12,
                                                  "title": "지갑을 습득했습니다",
                                                  "thumbnailImageUrl": "test@example.com",
                                                  "createdAt": "2026-02-20T14:32:10"
                                                },
                                                {
                                                  "postId": 9,
                                                  "title": "아이폰을 찾았습니다",
                                                  "thumbnailImageUrl": "test2@example.com",
                                                  "createdAt": "2026-02-19T11:20:00"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 에러",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<List<RecentFoundPostResponse>>> getRecentFoundPosts(@Valid @ModelAttribute RecentFoundPostRequest request,
                                                                                          @AuthenticationPrincipal UserDetails userDetails) {

        List<RecentFoundPostResponse> response = postMapService.getRecentFoundPosts(request, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{postId}/summary")
    @Operation(
            summary = "지도 게시글 카드 리스트 조회",
            description = """
                지도에서 특정 게시글 마커를 클릭했을 때 주변 게시글 카드 리스트를 조회합니다.
                
                - 기준 게시글의 위치를 중심으로 주변 게시글을 조회합니다.
                - 지도 레벨(level)에 따라 검색 반경이 결정됩니다.
                - 거리순으로 정렬됩니다.
                - 필터(postType, postStatus, category, keyword)를 적용할 수 있습니다.
                - 로그인 사용자인 경우 차단된 유저의 게시글은 제외됩니다.
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지도 게시글 카드 조회 성공",
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
                                              "id": 12,
                                              "title": "지갑을 습득했습니다",
                                              "summary": "검정색 지갑을 발견했습니다.",
                                              "thumbnailImageUrl": "https://image-url.com/1.jpg",
                                              "address": "서울시 중구 명동",
                                              "postStatus": "SEARCHING",
                                              "postType": "FOUND",
                                              "category": "WALLET",
                                              "favoriteCount": 4,
                                              "favoriteStatus": false,
                                              "viewCount": 120,
                                              "isNew": true,
                                              "isHot": false,
                                              "createdAt": "2026-02-20T14:32:10",
                                              "imageCount": 3
                                            }
                                          ]
                                        }
                                        """
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 에러",
                    content = @Content
            )
    })
    public ResponseEntity<ApiResponse<List<MapPostResponse>>> getPostsSummary(@PathVariable Long postId,
                                                                              @ModelAttribute @Valid MapPostRequest request,
                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        List<MapPostResponse> response = postMapService.getPostMap(postId, request, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
