package com.fmi.domain.post.web.controller;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.post.response.*;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.PostFilterDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/")
    @Operation(summary = "게시글 생성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart("request") CreatePostDto request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostResponse response = postService.createPost(request,userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/")
    @Operation(summary = "전체 게시글 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getAllPosts(
            @RequestParam Type type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        List<PostListResponse> posts = postService.getAllPosts(type, page, size);
        return ResponseEntity.ok(ApiResponse.onSuccess(posts));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId
    ){
        PostResponse post = postService.getPost(postId);
        return ResponseEntity.ok(ApiResponse.onSuccess(post));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestPart("request") UpdatePostDto request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostResponse response = postService.updatePost(postId, request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON403: 금지된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        postService.deletePost(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("게시글 삭제 완료"));
    }

    @PostMapping("/draft")
    @Operation(summary = "게시글 임시 저장")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 임시 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<Void>> saveTemporaryPost(

            @RequestPart("request") TemporaryPostDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        postService.saveTemporaryPost(request, userDetails, images);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @GetMapping("/draft")
    @Operation(summary = "임시 저장 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 저장 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<PostResponse>> getTemporaryPost(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PostResponse post = postService.getTemporaryPost(userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(post));
    }

    @DeleteMapping("/draft")
    @Operation(summary = "임시 저장 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임시 저장 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "COMMON401: 인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<String>> deleteTemporaryPost(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        postService.deleteTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("임시 게시글 삭제 완료"));
    }

    @GetMapping("/{postId}/share")
    @Operation(summary = "더미 데이터용 api")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 공유 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<PostShareResponse>> sharePost(@PathVariable Long postId){

        PostShareResponse shareResponse = postService.getSharePost(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(shareResponse));
    }

    @GetMapping("/views/{postId}")
    @Operation(summary = "게시글 조회수 증가",description = "한 유저당 시간 당 1 조회수로 제한")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회수 증가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "POST404-NOT_FOUND: 존재하지 않는 게시글입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<ViewResponse>> getPostView(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ViewResponse response = postService.getPostView(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/filter")
    @Operation(summary = "게시글 필터", description = "지역, 카테고리, 정렬, 찾음여부, 기간")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 필터 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "LIST400-INVALID_CURSOR: 유효하지 않은 커서입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ResponseEntity<ApiResponse<FilterResponse>> getFilter(@RequestBody PostFilterDto dto,
                                                                 @RequestParam(required = false) Long cursor,
                                                                 @PageableDefault(size = 20) Pageable pageable) {

        FilterResponse response = postService.getPostsByFilter(dto, pageable, cursor);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

}
