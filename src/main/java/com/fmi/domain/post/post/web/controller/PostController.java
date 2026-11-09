package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.service.PostDetailService;
import com.fmi.domain.post.post.service.PostQueryService;
import com.fmi.domain.post.post.service.PostService;
import com.fmi.domain.post.post.web.dto.request.*;
import com.fmi.domain.post.post.web.dto.response.*;
import com.fmi.domain.post.post.web.swagger.PostSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import com.fmi.global.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController implements PostSwagger {
    private final PostDetailService postDetailService;

    private final PostService postService;
    private final PostQueryService postQueryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestPart("request") @Valid PostCreateRequest request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostCreateResponse response = postService.createPost(request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/search")
    @Override
    public ResponseEntity<ApiResponse<PostPageResponse>> searchPostFilterOrSort(
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") int size) {
        PostPageResponse response = postQueryService.getPostListByFilterOrSort(
                postType, postStatus, category, address, sortType, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/search/keyword")
    @Override
    public ResponseEntity<ApiResponse<PostPageResponse>> searchByKeyword(
            @RequestParam String keyword,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") int size) {

        PostPageResponse postPageResponse = postQueryService.searchByKeyword(keyword, cursor, size, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(postPageResponse));
    }

    @GetMapping("/{postId}")
    @Override
    public ResponseEntity<ApiResponse<PostGetResponse>> getPost(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {

        String clientIp = (Objects.isNull(userDetails)) ? IpUtil.getClientIp(request) : null;

        PostGetResponse response = postDetailService.getPost(postId, userDetails, clientIp);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<PostUpdateResponse>> updatePost(
            @PathVariable Long postId,
            @RequestPart("request") PostUpdateRequest request,
            @RequestPart(value = "image", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostUpdateResponse response = postService.updatePost(postId, request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/{postId}")
    @Override
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {

        postService.deletePost(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess("게시글 삭제 완료"));
    }

    @PutMapping("/{postId}/radius")
    @Override
    public ResponseEntity<Void> updateRadius(
            @PathVariable Long postId, @RequestBody PostRadiusUpdateRequest request, UserDetails userDetails) {

        postService.updateRadius(postId, request, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}/similar")
    @Override
    public ResponseEntity<ApiResponse<List<PostSimilarResponse>>> getSimilarList(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        List<PostSimilarResponse> response = postQueryService.getSimilarPostList(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{postId}/share")
    @Override
    public ResponseEntity<ApiResponse<PostShareResponse>> sharePost(@PathVariable Long postId) {

        PostShareResponse shareResponse = postQueryService.getSharePost(postId);

        return ResponseEntity.ok(ApiResponse.onSuccess(shareResponse));
    }

    @PutMapping("/{postId}/status")
    @Override
    public ResponseEntity<ApiResponse<Void>> updateFound(
            @PathVariable Long postId,
            @RequestBody @Valid PostStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        postService.updatePostStatus(postId, request, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
