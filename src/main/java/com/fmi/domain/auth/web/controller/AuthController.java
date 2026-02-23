package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.response.SignupResponse;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.web.dto.LoginRequest;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final SocialAccountsRepository socialAccountsRepository;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;
    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;
    @Value("${jwt.cookie.secure:false}")
    private boolean cookieSecure;
    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임을 입력해 회원을 생성합니다. 비밀번호는 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 미인증",
                                            description = "AUTH400-EMAIL_NOT_VERIFIED",
                                            value = "{\"isSuccess\": false, \"code\": \"AUTH400-EMAIL_NOT_VERIFIED\", \"message\": \"이메일 인증이 완료되지 않았습니다. 이메일 인증을 먼저 완료해주세요.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "비밀번호 규칙 위반",
                                            description = "AUTH400-WEAK_PASSWORD",
                                            value = "{\"isSuccess\": false, \"code\": \"AUTH400-WEAK_PASSWORD\", \"message\": \"비밀번호 규칙을 만족하지 않습니다. 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다.\"}"
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "리소스 충돌",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "이메일 중복",
                                            description = "AUTH409-EMAIL_DUPLICATED",
                                            value = "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_DUPLICATED\", \"message\": \"이미 사용 중인 이메일입니다.\"}"
                                    ),
                                    @ExampleObject(
                                            name = "최근 탈퇴 이메일",
                                            description = "AUTH409-EMAIL_RECENTLY_DELETED",
                                            value = "{\"isSuccess\": false, \"code\": \"AUTH409-EMAIL_RECENTLY_DELETED\", \"message\": \"최근 탈퇴한 이메일입니다. 탈퇴 후 7일 이내 재가입할 수 없습니다.\"}"
                                    )
                            }
                    )
            )
    })
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signup(request);
        return ApiResponse.onSuccess(AuthConverter.toSignupResponse(id));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", 
               description = """
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
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH401-INVALID_CREDENTIALS\", \"message\": \"이메일 또는 비밀번호가 올바르지 않습니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var authResult = authService.authenticate(request.getEmail(), request.getPassword());
        var user = authResult.getUser();
        boolean isTemporaryPassword = authResult.isTemporaryPassword();
        
        var claims = new java.util.HashMap<String, Object>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("purpose", "access");
        if (isTemporaryPassword) {
            claims.put("isTemporaryPassword", true);  // JWT에 임시 비밀번호 플래그 포함
        }
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), claims);
        String jti = java.util.UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), jti);

        // refresh token 저장 (해시) 및 쿠키 설정 (쿠키 값은 리프레시 JWT)
        String refreshHash = sha256Hex(refreshToken);
        java.util.Date refreshExp = jwtTokenProvider.getExpiration(refreshToken);
        refreshTokenStore.issue(jti, user.getEmail(), refreshHash, refreshExp.toInstant());

        // accessToken 쿠키 설정
        java.util.Date accessExp = jwtTokenProvider.getExpiration(accessToken);
        ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), accessExp.toInstant()))
                .build();

        // refreshToken 쿠키 설정
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), refreshExp.toInstant()))
                .build();

        // 응답 생성 및 반환
        // accessToken과 refreshToken은 쿠키로 전송되므로 응답 body에는 포함하지 않습니다.
        // 응답 body에는 userId와 isTemporaryPassword만 포함됩니다.
        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())  // accessToken을 쿠키로 전송
                .header("Set-Cookie", refreshCookie.toString())  // refreshToken을 쿠키로 전송
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(user.getId(), isTemporaryPassword)));
                // 변경: accessToken 파라미터(null) 제거, userId와 isTemporaryPassword만 전달
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 유효성 및 중복 확인", 
            description = "닉네임 길이(2-10자), 금칙어, 중복 여부를 확인합니다. 부적절하거나 중복이면 400과 에러 메시지를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "NICKNAME_INVALID: 부적절한 닉네임 (길이, 금칙어 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NICKNAME_INVALID\", \"message\": \"부적절한 닉네임입니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "NICKNAME_DUPLICATE: 이미 사용 중인 닉네임",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"NICKNAME_DUPLICATE\", \"message\": \"중복된 닉네임입니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<?>> checkNickname(@RequestParam("nickname") @NotBlank String nickname) {
        var result = authService.checkNickname(nickname);
        
        if (!result.isAvailable()) {
            // 부적절한 닉네임 또는 중복된 닉네임
            return ResponseEntity.status(400).body(
                ApiResponse.onFailure(
                    "NICKNAME_" + result.getErrorType(), 
                    result.getMessage(), 
                    null
                )
            );
        }
        
        return ResponseEntity.ok(ApiResponse.onSuccess(AuthConverter.toCheckResponse(true)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 리프레시", description = "쿠키의 refresh_token(JWT)으로 액세스 토큰을 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 리프레시 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH401-INVALID_REFRESH: 리프레시 토큰이 없거나 유효하지 않습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH401-INVALID_REFRESH\", \"message\": \"리프레시 토큰이 없거나 유효하지 않습니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt == null || refreshJwt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "리프레시 토큰 없음", null));
        }

        if (!jwtTokenProvider.validateToken(refreshJwt)) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시", null));
        }

        String email = jwtTokenProvider.getSubject(refreshJwt);
        String jti = jwtTokenProvider.getJti(refreshJwt);
        if (jti == null || jti.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시(jti 없음)", null));
        }

        String hash = sha256Hex(refreshJwt);
        if (!refreshTokenStore.validate(jti, hash, email)) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시(대조 실패)", null));
        }

        var userOpt = authService.findActiveUserByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시(사용자 없음)", null));
        }
        var user = userOpt.get();

        // RTR: 기존 리프레시 폐기, 새 리프레시/쿠키 발급
        refreshTokenStore.revoke(jti);

        var claims = new java.util.HashMap<String, Object>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("purpose", "access");
        socialAccountsRepository.findByUser(user)
                .ifPresent(account -> claims.put("provider", account.getProvider().name()));
        String accessToken = jwtTokenProvider.createAccessToken(email, claims);

        String newJti = java.util.UUID.randomUUID().toString();
        String newRefresh = jwtTokenProvider.createRefreshToken(email, newJti);
        String newHash = sha256Hex(newRefresh);
        java.util.Date refreshExp = jwtTokenProvider.getExpiration(newRefresh);
        refreshTokenStore.issue(newJti, email, newHash, refreshExp.toInstant());

        // accessToken 쿠키 설정
        java.util.Date accessExp = jwtTokenProvider.getExpiration(accessToken);
        ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), accessExp.toInstant()))
                .build();

        // refreshToken 쿠키 설정
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, newRefresh)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), refreshExp.toInstant()))
                .build();

        // 응답 생성 및 반환
        // refresh() 메서드는 토큰 갱신만 수행하므로 userId는 null로 설정합니다.
        // accessToken과 refreshToken은 쿠키로 전송되므로 응답 body에는 포함하지 않습니다.
        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())  // 새 accessToken을 쿠키로 전송
                .header("Set-Cookie", refreshCookie.toString())  // 새 refreshToken을 쿠키로 전송
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(null)));
                // userId를 null로 전달: refresh()는 토큰 갱신만 하므로 userId 반환 불필요
                // LoginResponse 구조상 userId가 필수 필드라서 null 전달 (클라이언트는 사용하지 않음)
    }

    private static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "쿠키의 refresh_token(jti)을 폐기하고 쿠키를 제거합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt != null && !refreshJwt.isEmpty()) {
            if (jwtTokenProvider.validateToken(refreshJwt)) {
                String jti = jwtTokenProvider.getJti(refreshJwt);
                if (jti != null && !jti.isEmpty()) {
                    refreshTokenStore.revoke(jti);
                }
            }
        }
        
        // accessToken 쿠키 제거
        ResponseCookie removeAccess = ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        
        // refreshToken 쿠키 제거
        ResponseCookie removeRefresh = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        
        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess("OK"));
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
}


