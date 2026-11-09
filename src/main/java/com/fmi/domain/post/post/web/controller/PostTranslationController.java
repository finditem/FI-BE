package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.service.PostTranslationService;
import com.fmi.domain.post.post.web.dto.response.PostTranslationResponse;
import com.fmi.domain.post.post.web.swagger.PostTranslationSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostTranslationController implements PostTranslationSwagger {

    private final PostTranslationService postTranslationService;

    @GetMapping("/{postId}/translate")
    @Override
    public ResponseEntity<ApiResponse<PostTranslationResponse>> translatePost(
            @PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {

        PostTranslationResponse response = postTranslationService.getOrTranslate(postId, userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
