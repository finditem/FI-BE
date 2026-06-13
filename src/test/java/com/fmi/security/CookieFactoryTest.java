package com.fmi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieFactoryTest {

    private CookieFactory cookieFactory;

    @BeforeEach
    void setUp() {
        cookieFactory = new CookieFactory();
        ReflectionTestUtils.setField(cookieFactory, "secure", true);
        ReflectionTestUtils.setField(cookieFactory, "sameSite", "None");
        ReflectionTestUtils.setField(cookieFactory, "parentDomain", ".finditem.kr");
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    @Nested
    @DisplayName("Origin 기준 Domain 동적 결정")
    class ResolveDomain {

        @Test
        @DisplayName("Origin이 localhost면 Domain을 설정하지 않는다 (host-only)")
        void localhost_isHostOnly() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "http://localhost:3000");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isNull();
        }

        @Test
        @DisplayName("Origin이 release 서브도메인이면 부모 도메인을 부여한다")
        void releaseSubdomain_usesParentDomain() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://release.finditem.kr");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isEqualTo(".finditem.kr");
        }

        @Test
        @DisplayName("Origin이 운영 서브도메인이면 부모 도메인을 부여한다")
        void prodSubdomain_usesParentDomain() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://www.finditem.kr");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isEqualTo(".finditem.kr");
        }

        @Test
        @DisplayName("Origin이 부모 도메인 자체(finditem.kr)여도 부여한다")
        void bareParentDomain_usesParentDomain() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://finditem.kr");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isEqualTo(".finditem.kr");
        }

        @Test
        @DisplayName("부모 도메인을 가장한 위조 도메인은 부여하지 않는다")
        void spoofedDomain_isHostOnly() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://finditem.kr.evil.com");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isNull();
        }

        @Test
        @DisplayName("Origin이 없으면 Referer로 판단한다")
        void fallsBackToReferer() {
            MockHttpServletRequest req = request();
            req.addHeader("Referer", "https://release.finditem.kr/login");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isEqualTo(".finditem.kr");
        }

        @Test
        @DisplayName("Origin도 Referer도 없으면 Domain을 설정하지 않는다")
        void noOriginNoReferer_isHostOnly() {
            ResponseCookie cookie = cookieFactory.build(request(), "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isNull();
        }

        @Test
        @DisplayName("parent-domain 설정이 비어 있으면 finditem.kr Origin이어도 host-only")
        void blankParentDomain_alwaysHostOnly() {
            ReflectionTestUtils.setField(cookieFactory, "parentDomain", "");
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://release.finditem.kr");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.getDomain()).isNull();
        }
    }

    @Nested
    @DisplayName("쿠키 공통 속성")
    class CommonAttributes {

        @Test
        @DisplayName("httpOnly/secure/sameSite/path가 설정값대로 적용된다")
        void appliesBaseAttributes() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://release.finditem.kr");

            ResponseCookie cookie = cookieFactory.build(req, "access_token", "v", Duration.ofMinutes(15));

            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isTrue();
            assertThat(cookie.getSameSite()).isEqualTo("None");
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getName()).isEqualTo("access_token");
            assertThat(cookie.getValue()).isEqualTo("v");
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
        }
    }

    @Nested
    @DisplayName("expire - 쿠키 삭제")
    class Expire {

        @Test
        @DisplayName("빈 값 + MaxAge 0으로 만든다")
        void emptyValueZeroMaxAge() {
            ResponseCookie cookie = cookieFactory.expire(request(), "refresh_token");

            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("삭제 쿠키도 발급과 동일한 Domain 규칙을 따른다 (release)")
        void followsSameDomainRule() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "https://release.finditem.kr");

            ResponseCookie cookie = cookieFactory.expire(req, "refresh_token");

            assertThat(cookie.getDomain()).isEqualTo(".finditem.kr");
        }

        @Test
        @DisplayName("삭제 쿠키도 localhost면 host-only로 만든다")
        void localhostHostOnly() {
            MockHttpServletRequest req = request();
            req.addHeader("Origin", "http://localhost:3000");

            ResponseCookie cookie = cookieFactory.expire(req, "refresh_token");

            assertThat(cookie.getDomain()).isNull();
        }
    }
}
