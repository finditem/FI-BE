package com.fmi.domain.post.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.*;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("PostHotQueryRepositoryCustomImpl")
class PostHotQueryRepositoryCustomImplTest extends IntegrationTestSupport {
    @Autowired
    private PostHotQueryRepositoryCustomImpl repository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("인기 게시글을 조회할 때")
    class DescribeAddressFilter {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"은평", "서울특별시 은평구", "서울특별시 은평구 연서로 1", "   ", "광진"})
        @DisplayName("주소 일부나 전체가 일치하거나 지역 제한이 없으면 다른 필터에 맞는 게시글만 반환한다")
        void itFiltersByAddressAndOtherConditions(String address) {
            // given
            User author = userRepository.save(User.builder()
                    .nickname("주소검색작성자")
                    .email("address@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            Post expected = postRepository.save(Post.create(
                    "지갑",
                    "서울특별시 은평구 연서로 1",
                    37.6,
                    126.9,
                    PostType.LOST,
                    Category.WALLET,
                    "지갑 분실",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            postRepository.save(Post.create(
                    "가방",
                    "서울특별시 은평구 연서로 2",
                    37.6,
                    126.9,
                    PostType.LOST,
                    Category.BAG,
                    "가방 분실",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            postRepository.save(Post.create(
                    "지갑",
                    "서울특별시 은평구 연서로 3",
                    37.6,
                    126.9,
                    PostType.FOUND,
                    Category.WALLET,
                    "지갑 습득",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            Post otherRegion = postRepository.save(Post.create(
                    "지갑",
                    "서울특별시 성동구 성수이로 1",
                    37.54,
                    127.05,
                    PostType.LOST,
                    Category.WALLET,
                    "지갑 분실",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    author));
            postRepository.flush();
            // when
            var result = repository.findHotPostIds(PostType.LOST, PostStatus.SEARCHING, Category.WALLET, address, 10);
            // then
            Set<Long> expectedIds = Set.of(expected.getId());
            if (address == null || address.isBlank()) {
                expectedIds = Set.of(expected.getId(), otherRegion.getId());
            } else if (address.equals("광진")) {
                expectedIds = Set.of();
            }
            assertThat(result).containsExactlyInAnyOrderElementsOf(expectedIds);
        }
    }
}
