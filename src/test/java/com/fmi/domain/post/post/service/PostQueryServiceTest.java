package com.fmi.domain.post.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.web.dto.response.PostGetResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.util.IpUtil;
import com.fmi.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("PostQueryService")
class PostQueryServiceTest extends IntegrationTestSupport {
    @Autowired
    private PostQueryService postQueryService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("게시글 상세를 조회할 때")
    class DescribeGetPost {
        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("같은 회원 또는 비회원 IP의 당일 재조회면 조회수를 한 번만 증가시킨다")
        void itCountsDailyViewsOnce(boolean authenticated) {
            // given
            User author = userRepository.save(User.builder()
                    .profile_img("")
                    .nickname("조회수작성자")
                    .email("view-author@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            Post post = postRepository.save(Post.create(
                    "검은 지갑",
                    "서울 성동구",
                    37.5422,
                    127.0549,
                    PostType.LOST,
                    Category.WALLET,
                    "조회수 검증",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            UserDetails principal = authenticated
                    ? org.springframework.security.core.userdetails.User.withUsername(author.getEmail())
                            .password("password")
                            .authorities("ROLE_USER")
                            .build()
                    : null;
            String clientIp = "192.0.2.61";
            ZoneId zone = ZoneId.of("Asia/Seoul");
            String today = LocalDate.now(zone).format(DateTimeFormatter.BASIC_ISO_DATE);
            String key = "post:view:" + post.getId() + ":" + today + ":sic:"
                    + (authenticated ? "user" : "not-user") + ":value:"
                    + (authenticated ? author.getId().toString() : IpUtil.hashIp(clientIp));
            long secondsUntilMidnight = Duration.between(
                            LocalDateTime.now(zone),
                            LocalDate.now(zone).plusDays(1).atStartOfDay())
                    .getSeconds();
            entityManager.flush();
            entityManager.clear();

            // when
            PostGetResponse first = postQueryService.getPost(post.getId(), principal, clientIp);
            entityManager.clear();
            PostGetResponse second = postQueryService.getPost(post.getId(), principal, clientIp);
            entityManager.clear();

            // then
            assertThat(first.viewCount()).isEqualTo(1L);
            assertThat(second.viewCount()).isEqualTo(1L);
            assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount())
                    .isEqualTo(1L);
            assertThat(redis.opsForValue().get(key)).isEqualTo("1");
            assertThat(redis.getExpire(key)).isBetween(Math.max(0, secondsUntilMidnight - 30), secondsUntilMidnight);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", " ", "unknown", "UNKNOWN"})
        @DisplayName("비회원 IP가 없거나 unknown이면 조회수를 기록하지 않는다")
        void itSkipsUnknownGuestIp(String clientIp) {
            // given
            User author = userRepository.save(User.builder()
                    .profile_img("")
                    .nickname("비회원조회작성자")
                    .email("guest-view-author@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            Post post = postRepository.save(Post.create(
                    "지갑",
                    "서울 성동구",
                    37.5422,
                    127.0549,
                    PostType.LOST,
                    Category.WALLET,
                    "비회원 조회 검증",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            entityManager.flush();
            entityManager.clear();

            // when
            PostGetResponse result = postQueryService.getPost(post.getId(), null, clientIp);

            // then
            assertThat(result.viewCount()).isZero();
            assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount())
                    .isZero();
        }
    }
}
