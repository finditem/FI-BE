package com.fmi.global.web.controller;

import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.web.dto.request.ImageDeleteRequest;
import com.fmi.global.web.swagger.S3Swagger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/s3")
public class S3Controller implements S3Swagger {

    private final S3Service s3Service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ApiResponse<List<String>> s3Upload(@RequestPart(value = "image") List<MultipartFile> multipartFile) {
        List<String> upload = s3Service.upload(multipartFile);
        return ApiResponse.onSuccess(upload);
    }

    @DeleteMapping("/delete")
    @Override
    public ApiResponse<String> s3Delete(@RequestBody ImageDeleteRequest imageDeleteRequest) {
        s3Service.delete(imageDeleteRequest.getImageUrls());
        return ApiResponse.onSuccess("이미지 삭제 성공");
    }
}
