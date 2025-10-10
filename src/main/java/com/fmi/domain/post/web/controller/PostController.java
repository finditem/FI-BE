package com.fmi.domain.post.web.controller;

import com.fmi.domain.post.converter.PostConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.response.PostResponse;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.post.web.dto.CreatePostDto;
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
    private final PostConverter postConverter;

    @PostMapping(value = "/")
    @Operation(summary = "게시글 생성")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart("request") CreatePostDto request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostResponse response = postService.createPost(request,userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
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


    @PostMapping("/test-username")
    @Operation(summary = "userDetail.username 체크")
    public ResponseEntity<String> checkUsername(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("현재 로그인한 유저 username: {}", userDetails.getUsername());
        return ResponseEntity.ok("로그 확인");
    }

}
