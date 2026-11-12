package com.fmi.domain.post.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.PostFavorite;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.data.enums.Radius;
import com.fmi.domain.post.post.repository.PostFavoriteRepository;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.web.dto.response.PostBriefResponse;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.Role;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.enums.SortType;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("PostFavoriteService")
class PostFavoriteServiceTest extends IntegrationTestSupport {
    @Autowired
    private PostFavoriteService postFavoriteService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostFavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("내 즐겨찾기 목록을 조회할 때")
    class DescribeGetFavoritePost {
        @Test
        @DisplayName("내 활성 즐겨찾기 중 필터에 맞는 게시글을 커서 순서로 반환한다")
        void itReturnsFilteredActiveFavoritesWithCursor() {
            // given
            User requester = userRepository.save(User.builder()
                    .profile_img("")
                    .nickname("조회자")
                    .email("post-favorite-requester@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            User other = userRepository.save(User.builder()
                    .profile_img("")
                    .nickname("다른회원")
                    .email("post-favorite-other@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            UserDetails principal = org.springframework.security.core.userdetails.User.withUsername(
                            requester.getEmail())
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();
            Post older = null;
            Post newer = null;
            for (int i = 0; i < 8; i++) {
                Post post = postRepository.save(Post.create(
                        i == 7 ? "휴대폰" : "검은 지갑",
                        i == 6 ? "부산 해운대구" : "서울 성동구",
                        37.5422,
                        127.0549,
                        i == 4 ? PostType.FOUND : PostType.LOST,
                        i == 5 ? Category.ELECTRONICS : Category.WALLET,
                        "분실물 설명",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        other));
                PostFavorite favorite = favoriteRepository.save(PostFavorite.create(i == 3 ? other : requester, post));
                if (i == 0) older = post;
                if (i == 1) newer = post;
                if (i == 2) favorite.deactivate();
            }
            Post deleted = postRepository.save(Post.create(
                    "검은 지갑",
                    "서울 성동구",
                    37.5422,
                    127.0549,
                    PostType.LOST,
                    Category.WALLET,
                    "삭제된 분실물",
                    false,
                    LocalDateTime.of(2026, 9, 10, 10, 0),
                    Radius.DISTANCE_1000,
                    other));
            deleted.softDelete();
            favoriteRepository.save(PostFavorite.create(requester, deleted));

            // when
            PostPageResponse first = postFavoriteService.getFavoritePost(
                    principal, PostType.LOST, Category.WALLET, "서울", "지갑", SortType.LATEST, null, 1);
            PostPageResponse second = postFavoriteService.getFavoritePost(
                    principal, PostType.LOST, Category.WALLET, "서울", "지갑", SortType.LATEST, first.nextCursor(), 1);

            // then
            assertThat(first.postList()).extracting(PostBriefResponse::id).containsExactly(newer.getId());
            assertThat(first.postCount()).isEqualTo(2L);
            assertThat(first.hasNext()).isTrue();
            assertThat(first.nextCursor()).isEqualTo(newer.getId());
            assertThat(first.postList().get(0).favoriteStatus()).isTrue();
            assertThat(first.postList().get(0).favoriteCount()).isEqualTo(1L);
            assertThat(second.postList()).extracting(PostBriefResponse::id).containsExactly(older.getId());
            assertThat(second.postCount()).isEqualTo(2L);
            assertThat(second.hasNext()).isFalse();
            assertThat(second.nextCursor()).isNull();
            assertThat(postRepository.findById(newer.getId()).orElseThrow().getViewCount())
                    .isZero();
            assertThat(favoriteRepository.findByUserAndPost(requester, deleted))
                    .get()
                    .extracting(PostFavorite::isFavorite)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("즐겨찾기가 없으면 빈 목록과 종료 커서를 반환한다")
        void itReturnsEmptyPage() {
            // given
            User requester = userRepository.save(User.builder()
                    .profile_img("")
                    .nickname("빈목록조회자")
                    .email("post-favorite-empty@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            UserDetails principal = org.springframework.security.core.userdetails.User.withUsername(
                            requester.getEmail())
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            // when
            PostPageResponse result =
                    postFavoriteService.getFavoritePost(principal, null, null, null, null, SortType.LATEST, null, 20);

            // then
            assertThat(result.postList()).isEmpty();
            assertThat(result.postCount()).isZero();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("등록되지 않은 회원이면 USER404-NOT_FOUND를 반환한다")
        void itRejectsUnknownUser() {
            // given
            UserDetails principal = org.springframework.security.core.userdetails.User.withUsername(
                            "post-favorite-missing@test.com")
                    .password("password")
                    .authorities("ROLE_USER")
                    .build();

            // when
            Throwable thrown = catchThrowable(() ->
                    postFavoriteService.getFavoritePost(principal, null, null, null, null, SortType.LATEST, null, 20));

            // then
            assertThat(thrown)
                    .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                            .isEqualTo(ErrorStatus._USER_NOT_FOUND));
        }
    }
}
