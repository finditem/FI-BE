package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.service.PostFavoriteService;
import com.fmi.domain.post.post.web.dto.response.PostFavoriteResponse;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.post.post.web.swagger.PostFavoriteSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.enums.SortType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostFavoriteController implements PostFavoriteSwagger {
    private final PostFavoriteService postFavoriteService;

    @PostMapping("/posts/{postId}/favorites")
    @Override
    public ResponseEntity<ApiResponse<PostFavoriteResponse>> createFavorite(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {

        PostFavoriteResponse response = postFavoriteService.addFavorite(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/posts/{postId}/favorites")
    @Override
    public ResponseEntity<ApiResponse<PostFavoriteResponse>> deleteFavorite(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        PostFavoriteResponse response = postFavoriteService.deleteFavorite(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/users/me/favorites")
    @Override
    public ResponseEntity<ApiResponse<PostPageResponse>> getFavoritePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") int size) {
        PostPageResponse response = postFavoriteService.getFavoritePost(
                userDetails, postType, category, address, keyword, sortType, cursor, size);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
