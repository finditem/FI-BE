package com.fmi.global.web.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.service.S3Service;
import com.fmi.global.web.dto.ImageDeleteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/s3")
@Tag(name = "S3", description = "S3 테스트용 API")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping(value= "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "S3 이미지 업로드", description = "여러 장의 이미지를 S3에 업로드하고 URL을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ApiResponse<List<String>> s3Upload(@RequestPart(value = "image") List<MultipartFile> multipartFile) {
        List<String> upload = s3Service.upload(multipartFile);
        return ApiResponse.onSuccess(upload);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "S3 이미지 삭제", description = "S3에 저장된 이미지를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE400-URL_INVALID: 잘못된 URL 형식입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE404-NOT_FOUND: 존재하지 않는 파일입니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE500-DELETE_IO: 파일을 삭제할 수 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON500: 서버 에러")
    })
    public ApiResponse<String> s3Delete(@RequestBody ImageDeleteRequest imageDeleteRequest) {
        s3Service.delete(imageDeleteRequest.getImageUrls());
        return ApiResponse.onSuccess("이미지 삭제 성공");
    }

}
