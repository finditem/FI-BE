package com.fmi.domain.auth.web.swagger;

import com.fmi.domain.auth.web.dto.LoginRequest;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth")
public interface AuthSwagger {

    @Operation(
            summary = "회원가입",
            description =
                    "이메일/비밀번호/닉네임을 입력해 회원을 생성합니다. 비밀번호는 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다. 가입 성공 시 자동 로그인되어 토큰이 쿠키로 발급됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공 (자동 로그인 포함)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "이메일 미인증",
                                            description = "AUTH400-EMAIL_NOT_VERIFIED",
                                            value =
                                                    "{\"isSuccess\": false, \"code\": \"AUTH400-EMAIL_NOT_VERIFIED\", \"message\": \"이메일 인증이 완료되지 않았습니다. 이메일 인증을 먼저 완료해주세요.\"}"),
                                    @ExampleObject(
                                            name = "비밀번호 규칙 위반",
                                            description = "AUTH400-WEAK_PASSWORD",
                                            value =
                                                    "{\"isSuccess\": false, \"code\": \"AUTH400-WEAK_PASSWORD\", \"message\": \"비밀번호 규칙을 만족하지 않습니다. 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.\"}")
                                })),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "리소스 충돌",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "이메일 중복",
                                            description = "AUTH409-EMAIL_DUPLICATED",
                                            value =
                                                    "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_DUPLICATED\", \"message\": \"이미 사용 중인 이메일입니다.\"}"),
                                    @ExampleObject(
                                            name = "최근 탈퇴 이메일",
                                            description = "AUTH409-EMAIL_RECENTLY_DELETED",
                                            value =
                                                    "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_RECENTLY_DELETED\", \"message\": \"최근 탈퇴한 이메일입니다. 탈퇴 후 7일 이내 재가입할 수 없습니다.\"}")
                                }))
    })
    ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest);

    @Operation(summary = "로그인", description = """
                       이메일과 비밀번호로 로그인합니다.
                       - 일반 비밀번호 또는 임시 비밀번호로 로그인 가능합니다.
                       - 임시 비밀번호로 로그인한 경우 isTemporaryPassword가 true로 반환되며, 프론트에서 비밀번호 변경 페이지로 리다이렉트해야 합니다.
                       - 인증 실패 시 AUTH401-INVALID_CREDENTIALS가 반환됩니다.
                       """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "AUTH401-INVALID_CREDENTIALS: 이메일 또는 비밀번호가 올바르지 않습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH401-INVALID_CREDENTIALS\", \"message\": \"이메일 또는 비밀번호가 올바르지 않습니다.\"}")))
    })
    ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest);

    @Operation(
            summary = "닉네임 유효성 및 중복 확인",
            description = "닉네임 길이(2-10자), 금칙어, 중복 여부를 확인합니다. 부적절하거나 중복이면 400과 에러 메시지를 반환합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "NICKNAME_INVALID: 부적절한 닉네임 (길이, 금칙어 등)",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NICKNAME_INVALID\", \"message\": \"부적절한 닉네임입니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "NICKNAME_DUPLICATE: 이미 사용 중인 닉네임",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"NICKNAME_DUPLICATE\", \"message\": \"중복된 닉네임입니다.\"}")))
    })
    ResponseEntity<ApiResponse<?>> checkNickname(@RequestParam("nickname") @NotBlank String nickname);

    @Operation(summary = "토큰 리프레시", description = "쿠키의 refresh_token(JWT)으로 액세스 토큰을 갱신합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 리프레시 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "AUTH401-INVALID_REFRESH: 리프레시 토큰이 없거나 유효하지 않습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH401-INVALID_REFRESH\", \"message\": \"리프레시 토큰이 없거나 유효하지 않습니다.\"}")))
    })
    ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request);

    @Operation(summary = "로그아웃", description = "쿠키의 refresh_token(jti)을 폐기하고 쿠키를 제거합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")})
    ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request);
}
