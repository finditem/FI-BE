package com.fmi.domain.post.post.web.swagger;

import com.fmi.domain.post.post.web.dto.request.TemporaryPostCreateRequest;
import com.fmi.domain.post.post.web.dto.response.TemporaryPostResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Post", description = "게시글 관련 API")
public interface TemporaryPostSwagger {

    @Operation(summary = "임시 저장 게시글 생성/수정", description = """
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
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "임시 저장 성공",
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
                description = "잘못된 요청 (multipart 형식 오류/파라미터 오류)",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = @Content)
    })
    ResponseEntity<ApiResponse<Void>> saveTemporaryPost(
            TemporaryPostCreateRequest request,
            @Parameter(hidden = true) UserDetails userDetails,
            List<MultipartFile> images);

    @Operation(summary = "내 임시 저장 게시글 조회", description = """
                    로그인한 사용자의 임시 저장 게시글(temporarySave=true)을 조회합니다.

                    - 임시 저장 게시글이 없으면 result는 null로 반환됩니다.
                    - images에는 임시 저장 게시글에 연결된 이미지 목록이 포함됩니다. (id/imgUrl/imageType)
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(name = "임시 저장 존재", value = """
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
                                                    """),
                                    @ExampleObject(name = "임시 저장 없음", value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "성공",
                                                      "result": null
                                                    }
                                                    """)
                                })),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content)
    })
    ResponseEntity<ApiResponse<TemporaryPostResponse>> getTemporaryPost(
            @Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "내 임시 저장 게시글 삭제", description = """
                    로그인한 사용자의 임시 저장 게시글(temporarySave=true)을 삭제합니다.

                    - 임시 저장 게시글이 존재하면: 연결된 이미지(S3 + DB) 삭제 후 게시글을 삭제합니다.
                    - 임시 저장 게시글이 없으면: _TEMP_POST_NOT_FOUND 예외를 반환합니다.
                    """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공",
                content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "성공",
                                              "result": null
                                            }
                                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = @Content)
    })
    ResponseEntity<ApiResponse<Void>> deleteTemporaryPost(@Parameter(hidden = true) UserDetails userDetails);
}
