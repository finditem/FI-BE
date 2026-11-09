package com.fmi.domain.post.post.web.controller;

import com.fmi.domain.post.post.service.TemporaryPostService;
import com.fmi.domain.post.post.web.dto.request.TemporaryPostCreateRequest;
import com.fmi.domain.post.post.web.dto.response.TemporaryPostResponse;
import com.fmi.domain.post.post.web.swagger.TemporaryPostSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import java.util.List;
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
public class TemporaryPostController implements TemporaryPostSwagger {

    private final TemporaryPostService temporaryPostService;

    @PostMapping(value = "/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<ApiResponse<Void>> saveTemporaryPost(
            @RequestPart("request") TemporaryPostCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        temporaryPostService.create(request, userDetails, images);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @GetMapping("/temp")
    @Override
    public ResponseEntity<ApiResponse<TemporaryPostResponse>> getTemporaryPost(
            @AuthenticationPrincipal UserDetails userDetails) {
        TemporaryPostResponse response = temporaryPostService.getMyTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @DeleteMapping("/temp")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteTemporaryPost(@AuthenticationPrincipal UserDetails userDetails) {
        temporaryPostService.deleteTemporaryPost(userDetails);

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
