package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.service.PostMapService;
import com.fmi.domain.post.post.web.dto.request.LocationMapPostRequest;
import com.fmi.domain.post.post.web.dto.request.MapPostRequest;
import com.fmi.domain.post.post.web.dto.request.PostMarkerRequest;
import com.fmi.domain.post.post.web.dto.request.RecentFoundPostRequest;
import com.fmi.domain.post.post.web.dto.response.*;
import com.fmi.domain.post.post.web.swagger.MapSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/main/posts")
public class MapController implements MapSwagger {
    private final PostMapService postMapService;

    @GetMapping("/marker")
    @Override
    public ResponseEntity<ApiResponse<List<PostMarkerResponse>>> getPostsForMap(
            @Valid @ModelAttribute PostMarkerRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        List<PostMarkerResponse> response = postMapService.getPostMaker(request, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/recent-found")
    @Override
    public ResponseEntity<ApiResponse<List<RecentFoundPostResponse>>> getRecentFoundPosts(
            @Valid @ModelAttribute RecentFoundPostRequest request, @AuthenticationPrincipal UserDetails userDetails) {

        List<RecentFoundPostResponse> response = postMapService.getRecentFoundPosts(request, userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{postId}/summary")
    @Override
    public ResponseEntity<ApiResponse<MapPostPageResponse>> getPostsSummary(
            @PathVariable Long postId,
            @ModelAttribute @Valid MapPostRequest request,
            @RequestParam(required = false) Double lastDistance,
            @RequestParam(required = false) Long lastPostId,
            @AuthenticationPrincipal UserDetails userDetails) {
        MapPostPageResponse response =
                postMapService.getPostMap(postId, request, lastDistance, lastPostId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/search-location")
    @Override
    public ResponseEntity<ApiResponse<LocationMapPostPageResponse>> searchPostsByLocation(
            @ModelAttribute @Valid LocationMapPostRequest request,
            @RequestParam(required = false) Double lastDistance,
            @RequestParam(required = false) Long lastPostId,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocationMapPostPageResponse response =
                postMapService.searchPostsByLocation(request, lastDistance, lastPostId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
