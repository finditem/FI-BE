package com.fmi.global.web.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.service.S3Service;
import com.fmi.global.web.dto.ImageDeleteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping(value= "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<String>> s3Upload(@RequestPart(value = "image") List<MultipartFile> multipartFile) {
        List<String> upload = s3Service.upload(multipartFile);
        return ApiResponse.onSuccess(upload);
    }

    @DeleteMapping("/delete")
    public ApiResponse<String> s3Delete(@RequestBody ImageDeleteRequest imageDeleteRequest) {
        s3Service.delete(imageDeleteRequest.getImageUrls());
        return ApiResponse.onSuccess("이미지 삭제 성공");
    }

}
