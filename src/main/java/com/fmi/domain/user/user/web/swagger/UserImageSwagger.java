package com.fmi.domain.user.user.web.swagger;

import com.fmi.domain.user.user.web.dto.response.ImageUploadResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 정보 관리 API")
public interface UserImageSwagger {

    @Operation(summary = "이미지 업로드", description = "여러 장의 이미지를 S3에 업로드하고 URL을 반환합니다. (JPEG, PNG 형식만 지원)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"FILE400-EXT_MISSING\", \"message\": \"확장자가 존재하지 않습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "415",
                description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"FILE415-EXT_UNSUPPORTED\", \"message\": \"허용되지 않는 확장자입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"FILE500-UPLOAD_IO\", \"message\": \"업로드 중 오류가 발생했습니다.\"}")))
    })
    ApiResponse<ImageUploadResponse> uploadImages(@RequestPart(value = "images") List<MultipartFile> images);
}
