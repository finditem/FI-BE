package com.fmi.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

/**
 * 인증 쿠키를 생성/삭제
 *     Origin 호스트가 .finditem.kr 에 속하면 → 해당 부모 도메인 부여
 *     localhost → Domain 미설정(host-only)
 */
@Component
public class CookieFactory {

    @Value("${jwt.cookie.secure:false}")
    private boolean secure;
    @Value("${jwt.cookie.same-site:Lax}")
    private String sameSite;
    /** 서브도메인 간 공유시킬 부모 도메인(.finditem.kr). 비어 있으면 항상 host-only. */
    @Value("${jwt.cookie.parent-domain:}")
    private String parentDomain;

    public ResponseCookie build(HttpServletRequest request, String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge);

        String domain = resolveDomain(request);
        if (domain != null) {
            builder.domain(domain);
        }
        // domain == null 이면 Domain 미설정 → host-only (로컬 환경)
        return builder.build();
    }

    /** 빈 값+MaxAge=0 쿠키를 만들어 정상 삭제 */
    public ResponseCookie expire(HttpServletRequest request, String name) {
        return build(request, name, "", Duration.ZERO);
    }

    private String resolveDomain(HttpServletRequest request) {
        if (parentDomain == null || parentDomain.isBlank() || request == null) {
            return null;
        }

        String host = hostOf(request.getHeader("Origin"));
        if (host == null) {
            host = hostOf(request.getHeader("Referer")); // Origin이 없는 엣지케이스 대비
        }
        if (host == null) {
            return null;
        }

        String bare = parentDomain.startsWith(".") ? parentDomain.substring(1) : parentDomain;
        boolean belongsToParent = host.equals(bare) || host.endsWith("." + bare);
        return belongsToParent ? parentDomain : null;
    }

    private String hostOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }
}
