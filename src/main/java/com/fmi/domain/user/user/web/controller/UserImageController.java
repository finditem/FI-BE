package com.fmi.domain.user.user.web.controller;

import com.fmi.domain.user.user.service.UserImageService;
import com.fmi.domain.user.user.web.dto.response.ImageUploadResponse;
import com.fmi.domain.user.user.web.swagger.UserImageSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserImageController implements UserImageSwagger {
    private final UserImageService userImageService;

    @PostMapping("/uploads/images")
    @Override
    public ApiResponse<ImageUploadResponse> uploadImages(@RequestPart(value = "images") List<MultipartFile> images) {
        ImageUploadResponse response = userImageService.uploadImages(images);
        return ApiResponse.onSuccess(response);
    }
}
