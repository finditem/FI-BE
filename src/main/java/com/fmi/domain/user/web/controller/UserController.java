package com.fmi.domain.user.web.controller;

import com.fmi.domain.Enum.ActivityType;
import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.SortType;
import com.fmi.domain.Enum.UserOtherPageType;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.web.dto.response.PostPageResponse;
import com.fmi.domain.user.response.ImageUploadResponse;
import com.fmi.domain.user.response.MyCommentPageResponse;
import com.fmi.domain.user.response.MyActivityPageResponse;
import com.fmi.domain.user.response.UserOtherPageResponse;
import com.fmi.domain.user.response.UserProfileResponse;
import com.fmi.domain.user.service.UserService;
import com.fmi.domain.user.web.dto.AccountDeleteRequest;
import com.fmi.domain.user.web.dto.PasswordChangeRequest;
import com.fmi.domain.user.web.dto.PasswordVerifyRequest;
import com.fmi.domain.user.web.dto.UserUpdateRequest;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 정보 관리 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/uploads/images")
    @Operation(summary = "이미지 업로드", description = "여러 장의 이미지를 S3에 업로드하고 URL을 반환합니다. (JPEG, PNG 형식만 지원)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "FILE400-EXT_MISSING: 확장자가 존재하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"FILE400-EXT_MISSING\", \"message\": \"확장자가 존재하지 않습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "415",
                    description = "FILE415-EXT_UNSUPPORTED: 허용되지 않는 확장자입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"FILE415-EXT_UNSUPPORTED\", \"message\": \"허용되지 않는 확장자입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "FILE500-UPLOAD_IO: 업로드 중 오류가 발생했습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"FILE500-UPLOAD_IO\", \"message\": \"업로드 중 오류가 발생했습니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<ImageUploadResponse> uploadImages(
            @RequestPart(value = "images") List<MultipartFile> images
    ) {
        ImageUploadResponse response = userService.uploadImages(images);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 프로필 정보를 조회합니다. (닉네임, 이메일, 프로필 이미지)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.getMyProfile(email);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me/comments")
    @Operation(summary = "내가 쓴 댓글 목록", description = "현재 로그인한 사용자가 작성한 댓글 목록을 커서 기반으로 조회합니다. 삭제된 댓글은 제외됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 댓글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    public ApiResponse<MyCommentPageResponse> getMyComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = userDetails.getUsername();
        MyCommentPageResponse response = userService.getMyComments(email, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me/posts")
    @Operation(summary = "내가 쓴 게시글 목록", description = """
            현재 로그인한 사용자가 작성한 게시글 목록을 커서 기반으로 조회합니다.

            **필터 파라미터** (모두 선택):
            - postType: LOST / FOUND
            - postStatus: SEARCHING / FOUND
            - category: ELECTRONICS / WALLET / ID_CARD / JEWELRY / BAG / CARD / ETC
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 게시글 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    public ApiResponse<PostPageResponse> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) PostStatus postStatus,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false, defaultValue = "LATEST") SortType sortType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        String email = userDetails.getUsername();
        PostPageResponse response = userService.getMyPosts(email, postType, postStatus, category, sortType, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/me/activities")
    @Operation(summary = "내 활동 내역 통합 조회", description = """
            현재 로그인한 사용자의 모든 활동 내역을 통합하여 최신순으로 조회합니다.

            **활동 유형 (type):**
            - POST: 작성한 게시글
            - COMMENT: 작성한 댓글
            - FAVORITE: 즐겨찾기한 게시글
            - INQUIRY: 문의 내역
            - REPORT: 신고 내역
            - VERIFICATION: 인증 내역
            - 미지정 시 전체 활동 조회

            **날짜 필터:**
            - startDate, endDate: yyyy-MM-dd 형식 (예: 2024-01-01)

            **커서**: ISO datetime 형식 (예: 2024-01-01T00:00:00)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활동 내역 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다")
    })
    public ApiResponse<MyActivityPageResponse> getMyActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        String email = userDetails.getUsername();
        MyActivityPageResponse response = userService.getMyActivities(email, type, startDate, endDate, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @GetMapping("/{userId}/page")
    @Operation(summary = "타인 페이지 조회", description = """
            다른 사용자의 닉네임, 프로필 이미지, 게시글, 작성 댓글, 즐겨찾기 목록을 조회합니다.

            **type 파라미터:**
            - type=posts → 게시글만 조회
            - type=comments → 댓글만 조회
            - type=favorites → 즐겨찾기만 조회
            - type 미지정 → 기본 탭(posts) 조회

            **페이지네이션:**
            - cursor: 다음 페이지 시작점 (이전 응답의 nextCursor 값)
            - size: 한 페이지당 항목 수 (기본값 10)
            - 응답의 hasNext가 true이면 nextCursor로 다음 페이지 요청 가능
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "타인 페이지 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "USER400-PAGE_TYPE_INVALID: type 파라미터는 posts, comments, favorites 중 하나여야 합니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER400-PAGE_TYPE_INVALID\", \"message\": \"type 파라미터는 posts, comments, favorites 중 하나여야 합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<UserOtherPageResponse> getUserOtherPage(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "posts") UserOtherPageType type,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserOtherPageResponse response = userService.getOtherUserPage(userId, type, userDetails, cursor, size);
        return ApiResponse.onSuccess(response);
    }

    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "내 정보 수정", description = """
            현재 로그인한 사용자의 프로필 정보를 수정합니다. (닉네임, 프로필 이미지 통합)

            **닉네임** (request JSON 내 nickname 필드):
            - 필드 자체를 보내지 않으면 → 닉네임 변경 없음
            - 값을 보내면 → 닉네임 변경 (2~15자)
            
            **프로필 이미지** (profileImage 파일 파트):
            - 파일을 보내지 않으면 → 이미지 변경 없음
            - 파일을 보내면 → 기존 이미지 삭제 후 새 이미지로 업데이트
            
            **이미지 삭제** (deleteProfileImage 파라미터):
            - true로 보내면 → 기존 이미지 삭제 (프로필 이미지 없음 상태)
            """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 정보 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "AUTH409-NICKNAME_DUPLICATED: 이미 사용 중인 닉네임입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH409-NICKNAME_DUPLICATED\", \"message\": \"이미 사용 중인 닉네임입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(value = "request", required = false) @Valid UserUpdateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "deleteProfileImage", required = false, defaultValue = "false") boolean deleteProfileImage
    ) {
        String email = userDetails.getUsername();
        UserProfileResponse response = userService.updateMyProfile(email, request, profileImage, deleteProfileImage);
        return ApiResponse.onSuccess(response);
    }

    @PostMapping("/me/password/verify")
    @Operation(summary = "현재 비밀번호 검증",
            description = "현재 비밀번호가 올바른지 검증합니다. 비밀번호 변경 전에 먼저 호출하여 비밀번호를 확인해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "USER400-PASSWORD_INCORRECT: 현재 비밀번호가 일치하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER400-PASSWORD_INCORRECT\", \"message\": \"현재 비밀번호가 일치하지 않습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordVerifyRequest request
    ) {
        String email = userDetails.getUsername();
        userService.verifyPasswordWithException(email, request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/me/password")
    @Operation(summary = "비밀번호 변경",
            description = "새 비밀번호로 변경합니다. 비밀번호 검증은 별도 엔드포인트(/users/me/password/verify)에서 먼저 완료해야 합니다. 새 비밀번호는 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "USER400-PASSWORD_MISMATCH: 새 비밀번호와 확인이 일치하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER400-PASSWORD_MISMATCH\", \"message\": \"새 비밀번호와 확인이 일치하지 않습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "AUTH400-WEAK_PASSWORD: 비밀번호 규칙을 만족하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH400-WEAK_PASSWORD\", \"message\": \"비밀번호 규칙을 만족하지 않습니다. 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        String email = userDetails.getUsername();
        userService.changePassword(email, request);
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴",
            description = """
                    현재 로그인한 사용자의 계정을 소프트 삭제합니다.
                    
                    **삭제 방식:**
                    - 즉시 소프트 삭제: 계정은 삭제 표시되지만 30일간 데이터가 보관됩니다
                    - 프로필 이미지는 즉시 S3에서 삭제됩니다
                    - 30일 후 자동으로 완전 삭제(하드 삭제)됩니다
                    
                    **복구 및 재가입:**
                    - 30일 이내에는 복구가 가능합니다 (관리자 문의)
                    - 탈퇴 후 7일 이내에는 동일한 이메일로 재가입할 수 없습니다
                    - 7일 경과 후에는 동일한 이메일로 재가입이 가능합니다
                    
                    **주의사항:**
                    - 탈퇴 후에는 로그인 및 서비스 이용이 불가능합니다
                    - 작성한 게시글과 댓글은 자동으로 삭제되지 않으며, 익명화 처리될 수 있습니다
                    
                    **탈퇴 사유:**
                    - 탈퇴 사유를 선택해야 합니다
                    - reason이 OTHER인 경우 otherReason에 상세 사유를 입력할 수 있습니다
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "USER404-NOT_FOUND: 존재하지 않는 회원입니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"존재하지 않는 회원입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "FILE500-DELETE_IO: 파일을 삭제할 수 없습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"FILE500-DELETE_IO\", \"message\": \"파일을 삭제할 수 없습니다.\"}"
                            )
                    )
            )
    })
    public ApiResponse<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountDeleteRequest request
    ) {
        String email = userDetails.getUsername();
        userService.deleteAccount(email, request);
        return ApiResponse.onSuccess(null);
    }
}

