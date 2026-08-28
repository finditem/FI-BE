package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.service.TokenIssuer;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.auth.web.dto.LoginRequest;
import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.PasswordVerifyRequest;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.user.service.NicknameService;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final NicknameService nicknameService;
    private final TokenIssuer tokenIssuer;
    private final PasswordService passwordService;
    private final WithdrawalService withdrawalService;
    private final CookieFactory cookieFactory;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;

    @PostMapping("/auth/signup")
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
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        var user = authService.signup(request);
        return buildTokenResponse(httpRequest, user, false);
    }

    @PostMapping("/auth/login")
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
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        var authResult = authService.authenticate(request.getEmail(), request.getPassword());
        return buildTokenResponse(httpRequest, authResult.getUser(), authResult.isTemporaryPassword());
    }

    @GetMapping("/auth/check-nickname")
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
    public ResponseEntity<ApiResponse<?>> checkNickname(@RequestParam("nickname") @NotBlank String nickname) {
        var result = nicknameService.check(nickname);

        if (!result.available()) {
            // 부적절한 닉네임 또는 중복된 닉네임
            return ResponseEntity.status(400)
                    .body(ApiResponse.onFailure("NICKNAME_" + result.errorType(), result.message(), null));
        }

        return ResponseEntity.ok(ApiResponse.onSuccess(AuthConverter.toCheckResponse(true)));
    }

    @PostMapping("/auth/refresh")
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
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt == null || refreshJwt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "리프레시 토큰 없음", null));
        }

        TokenIssuer.RefreshResult refreshResult = tokenIssuer.refresh(refreshJwt);
        if (!refreshResult.isSuccess()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure(
                            "AUTH401-INVALID_REFRESH", refreshFailureMessage(refreshResult.failure()), null));
        }

        TokenIssuer.IssuedTokens issuedTokens = refreshResult.issuedTokens();

        ResponseCookie accessCookie =
                buildCookie(request, accessCookieName, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie =
                buildCookie(request, refreshCookieName, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(null)));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> buildTokenResponse(
            HttpServletRequest request, com.fmi.domain.user.data.User user, boolean isTemporaryPassword) {
        TokenIssuer.IssuedTokens issuedTokens = tokenIssuer.issue(user, isTemporaryPassword, null);

        ResponseCookie accessCookie =
                buildCookie(request, accessCookieName, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie =
                buildCookie(request, refreshCookieName, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(user.getId(), isTemporaryPassword)));
    }

    private ResponseCookie buildCookie(
            HttpServletRequest request, String name, String value, java.util.Date expiration) {
        return cookieFactory.build(
                request, name, value, java.time.Duration.between(java.time.Instant.now(), expiration.toInstant()));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "로그아웃", description = "쿠키의 refresh_token(jti)을 폐기하고 쿠키를 제거합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")})
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt != null && !refreshJwt.isEmpty()) {
            tokenIssuer.revokeIfValid(refreshJwt);
        }

        // accessToken 쿠키 제거
        ResponseCookie removeAccess = cookieFactory.expire(request, accessCookieName);

        // refreshToken 쿠키 제거
        ResponseCookie removeRefresh = cookieFactory.expire(request, refreshCookieName);

        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess("OK"));
    }

    @PostMapping("/users/me/password/verify")
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordVerifyRequest request) {
        passwordService.verify(userDetails.getUsername(), request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/users/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.change(userDetails.getUsername(), request.getNewPassword(), request.getNewPasswordConfirm());
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest httpRequest) {
        withdrawalService.delete(userDetails.getUsername(), request);
        ResponseCookie removeAccess = cookieFactory.expire(httpRequest, accessCookieName);
        ResponseCookie removeRefresh = cookieFactory.expire(httpRequest, refreshCookieName);
        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess(null));
    }

    private static String getCookieValue(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private static String refreshFailureMessage(TokenIssuer.RefreshFailure failure) {
        return switch (failure) {
            case INVALID_TOKEN -> "유효하지 않은 리프레시";
            case MISSING_JTI -> "유효하지 않은 리프레시(jti 없음)";
            case HASH_MISMATCH -> "유효하지 않은 리프레시(대조 실패)";
            case USER_NOT_FOUND -> "유효하지 않은 리프레시(사용자 없음)";
        };
    }
}
