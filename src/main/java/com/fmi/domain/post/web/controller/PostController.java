package com.fmi.domain.post.web.controller;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.post.response.PostListResponse;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.response.PostShareResponse;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.web.dto.CreatePostDto;
import com.fmi.domain.post.web.dto.TemporaryPostDto;
import com.fmi.domain.post.web.dto.UpdatePostDto;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart("request") CreatePostDto request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostResponse response = postService.createPost(request,userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/")
    @Operation(summary = "전체 게시글 조회")
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
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId
    ){
        PostResponse post = postService.getPost(postId);
        return ResponseEntity.ok(ApiResponse.onSuccess(post));
    }

    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정")
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
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {

        postService.deletePost(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("게시글 삭제 완료"));
    }

    @PostMapping("/draft")
    @Operation(summary = "게시글 임시 저장")
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
    public ResponseEntity<ApiResponse<PostResponse>> getTemporaryPost(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PostResponse post = postService.getTemporaryPost(userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(post));
    }

    @DeleteMapping("/draft")
    @Operation(summary = "임시 저장 삭제")
    public ResponseEntity<ApiResponse<String>> deleteTemporaryPost(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        postService.deleteTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("임시 게시글 삭제 완료"));
    }

    @GetMapping("/{postId}/share")
    @Operation(summary = "게시글 공유")
    public ResponseEntity<ApiResponse<PostShareResponse>> sharePost(@PathVariable Long postId){

        PostShareResponse shareResponse = postService.getSharePost(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(shareResponse));
    }

}
