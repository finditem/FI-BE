package com.fmi.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.web.dto.request.PostCreateRequest;
import com.fmi.domain.post.web.dto.response.PostCreateResponse;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.repository.TemperatureActivityRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("PostService 게시글 생성")
class PostServiceCreateTest extends IntegrationTestSupport {

    @Autowired
    private PostService postService;

    @Autowired
    private TemperatureActivityRepository temperatureActivityRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("게시글을 생성할 때")
    class DescribeCreatePost {

        @Test
        @DisplayName("습득 게시글이면 작성자에게 습득 게시 활동을 기록한다")
        void itRecordsFoundPost() {
            // given
            User author = saveUser("create-found@test.com");

            // when
            PostCreateResponse result = postService.createPost(request(PostType.FOUND), userDetails(author), null);

            // then
            assertThat(temperatureActivityRepository.findAll().stream()
                            .filter(activity -> activity.getUserId().equals(author.getId())))
                    .singleElement()
                    .satisfies(activity -> {
                        assertThat(activity.getActivityType()).isEqualTo(TemperatureActivityType.FOUND_POST);
                        assertThat(activity.getDeltaTenths()).isEqualTo(5);
                    })
                    .extracting("sourceId")
                    .isEqualTo(result.id());
        }

        @Test
        @DisplayName("분실 게시글이면 활동을 기록하지 않는다")
        void itSkipsLostPost() {
            // given
            User author = saveUser("create-lost@test.com");

            // when
            postService.createPost(request(PostType.LOST), userDetails(author), null);

            // then
            assertThat(temperatureActivityRepository.findAll().stream()
                            .filter(activity -> activity.getUserId().equals(author.getId())))
                    .isEmpty();
        }
    }

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .nickname("게시글작성자")
                .email(email)
                .password("password")
                .role(Role.USER)
                .build());
    }

    private UserDetails userDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password("password")
                .build();
    }

    private PostCreateRequest request(PostType postType) {
        return new PostCreateRequest(
                postType,
                "지갑",
                LocalDateTime.of(2026, 9, 10, 10, 0),
                "서울 성동구",
                37.5422,
                127.0549,
                "검은색 지갑입니다.",
                Radius.DISTANCE_1000,
                Category.WALLET);
    }
}
