package com.fmi.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewSummary;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("ReviewRepository")
class ReviewRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FindCompletionRepository findCompletionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("받은 후기를 조회할 때")
    class DescribeFindReceivedReviews {

        @Nested
        @DisplayName("작성일시가 같은 후기가 있으면")
        class ContextWithSameCreatedAt {

            @Test
            @DisplayName("작성일시 내림차순, 같으면 id 내림차순으로 정렬한다")
            void itOrdersByCreatedAtAndId() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("목록작성자")
                        .email("order-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("목록수신자")
                        .email("order-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(930001L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }
                reviewRepository.flush();
                jdbcTemplate.update(
                        "UPDATE review SET created_at = ? WHERE id = ?",
                        LocalDateTime.of(2026, 9, 1, 10, 0),
                        reviews.get(0).getId());
                jdbcTemplate.update(
                        "UPDATE review SET created_at = ? WHERE id IN (?, ?)",
                        LocalDateTime.of(2026, 9, 2, 10, 0),
                        reviews.get(1).getId(),
                        reviews.get(2).getId());

                // when
                List<ReviewSummary> result = reviewRepository.findReceivedReviews(recipient.getId(), true, null, 10);

                // then
                assertThat(result)
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(
                                reviews.get(2).getId(),
                                reviews.get(1).getId(),
                                reviews.get(0).getId());
            }
        }

        @Nested
        @DisplayName("커서를 주면")
        class ContextWithCursor {

            @Test
            @DisplayName("작성일시가 같은 후기도 빠짐없이 커서 다음 후기부터 조회한다")
            void itReturnsReviewsAfterCursor() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("목록작성자")
                        .email("cursor-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("목록수신자")
                        .email("cursor-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(930101L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }
                reviewRepository.flush();
                jdbcTemplate.update(
                        "UPDATE review SET created_at = ? WHERE id IN (?, ?, ?)",
                        LocalDateTime.of(2026, 9, 2, 10, 0),
                        reviews.get(0).getId(),
                        reviews.get(1).getId(),
                        reviews.get(2).getId());

                // when
                List<ReviewSummary> result = reviewRepository.findReceivedReviews(
                        recipient.getId(), true, reviews.get(2).getId(), 10);

                // then
                assertThat(result)
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(1).getId(), reviews.get(0).getId());
            }
        }

        @Nested
        @DisplayName("받은 사용자가 숨긴 후기가 있으면")
        class ContextWithHiddenByRecipient {

            @Test
            @DisplayName("숨김 제외 조건일 때만 목록과 개수에서 뺀다")
            void itExcludesHiddenOnlyWhenRequested() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("목록작성자")
                        .email("hidden-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("목록수신자")
                        .email("hidden-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(930201L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }
                reviews.get(0).hideByRecipient();
                reviewRepository.flush();

                // when
                List<ReviewSummary> excluded = reviewRepository.findReceivedReviews(recipient.getId(), true, null, 10);
                List<ReviewSummary> included = reviewRepository.findReceivedReviews(recipient.getId(), false, null, 10);

                // then
                assertThat(excluded)
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(1).getId());
                assertThat(reviewRepository.countReceivedReviews(recipient.getId(), true))
                        .isEqualTo(1);
                assertThat(included).hasSize(2);
                assertThat(reviewRepository.countReceivedReviews(recipient.getId(), false))
                        .isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("작성자가 탈퇴했으면")
        class ContextWithWithdrawnAuthor {

            @Test
            @DisplayName("탈퇴 직후와 하드 삭제 뒤 모두 목록과 개수에 포함한다")
            void itIncludesReviewsOfWithdrawnAuthors() {
                // given
                User softDeletedAuthor = userRepository.save(User.builder()
                        .nickname("탈퇴작성자")
                        .email("withdrawn-soft@test.com")
                        .password("password")
                        .role(Role.USER)
                        .deletedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                        .build());
                User hardDeletedAuthor = userRepository.save(User.builder()
                        .nickname("하드삭제작성자")
                        .email("withdrawn-harddeletedauthor@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("탈퇴후기수신자")
                        .email("withdrawn-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion softCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(930301L)
                        .requester(softDeletedAuthor)
                        .helper(recipient)
                        .build());
                Review softReview = reviewRepository.save(Review.builder()
                        .findCompletion(softCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE))
                        .build());
                FindCompletion hardCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(930302L)
                        .requester(hardDeletedAuthor)
                        .helper(recipient)
                        .build());
                Review hardReview = reviewRepository.save(Review.builder()
                        .findCompletion(hardCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());
                reviewRepository.flush();
                jdbcTemplate.update("UPDATE review SET author_id = NULL WHERE id = ?", hardReview.getId());

                // when
                List<ReviewSummary> result = reviewRepository.findReceivedReviews(recipient.getId(), true, null, 10);

                // then
                assertThat(result).hasSize(2).allMatch(ReviewSummary::isUserWithdrawn);
                assertThat(result)
                        .extracting(ReviewSummary::reviewId)
                        .containsExactlyInAnyOrder(softReview.getId(), hardReview.getId());
                assertThat(reviewRepository.countReceivedReviews(recipient.getId(), true))
                        .isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("삭제 상태인 후기가 있으면")
        class ContextWithDeletedReview {

            @Test
            @DisplayName("목록과 개수에서 뺀다")
            void itExcludesDeletedReviews() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("목록작성자")
                        .email("deleted-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("목록수신자")
                        .email("deleted-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(930401L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }
                reviews.get(0).delete(LocalDateTime.of(2026, 9, 3, 10, 0));
                reviewRepository.flush();

                // when
                List<ReviewSummary> result = reviewRepository.findReceivedReviews(recipient.getId(), true, null, 10);

                // then
                assertThat(result)
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(1).getId());
                assertThat(reviewRepository.countReceivedReviews(recipient.getId(), true))
                        .isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("보낸 후기를 조회할 때")
    class DescribeFindSentReviews {

        @Nested
        @DisplayName("작성자가 숨긴 후기가 있으면")
        class ContextWithHiddenByAuthor {

            @Test
            @DisplayName("받은 사용자를 상대방으로 조회하고 숨긴 후기는 목록과 개수에서 뺀다")
            void itExcludesHiddenByAuthor() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("목록작성자")
                        .email("sent-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("목록수신자")
                        .email("sent-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(930501L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }
                reviews.get(0).hideByAuthor();
                reviewRepository.flush();

                // when
                List<ReviewSummary> result = reviewRepository.findSentReviews(author.getId(), null, 10);

                // then
                assertThat(result).singleElement().satisfies(summary -> {
                    assertThat(summary.reviewId()).isEqualTo(reviews.get(1).getId());
                    assertThat(summary.userId()).isEqualTo(recipient.getId());
                    assertThat(summary.nickname()).isEqualTo("목록수신자");
                });
                assertThat(reviewRepository.countSentReviews(author.getId())).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("도움 경험을 조회할 때")
    class DescribeFindHelpTypesByReviewIds {

        @Test
        @DisplayName("후기별로 선택한 도움 경험을 묶어서 반환한다")
        void itGroupsHelpTypesByReview() {
            // given
            User author = userRepository.save(User.builder()
                    .nickname("목록작성자")
                    .email("help-author@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            User recipient = userRepository.save(User.builder()
                    .nickname("목록수신자")
                    .email("help-recipient@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            FindCompletion firstCompletion = findCompletionRepository.save(FindCompletion.builder()
                    .sourcePostId(930601L)
                    .requester(author)
                    .helper(recipient)
                    .build());
            Review first = reviewRepository.save(Review.builder()
                    .findCompletion(firstCompletion)
                    .emotion(ReviewEmotion.GRATEFUL)
                    .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM, ReviewHelpType.QUICK_RESPONSE))
                    .build());
            FindCompletion secondCompletion = findCompletionRepository.save(FindCompletion.builder()
                    .sourcePostId(930602L)
                    .requester(author)
                    .helper(recipient)
                    .build());
            Review second = reviewRepository.save(Review.builder()
                    .findCompletion(secondCompletion)
                    .emotion(ReviewEmotion.MOVED)
                    .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE))
                    .build());
            reviewRepository.flush();

            // when
            Map<Long, Set<ReviewHelpType>> result =
                    reviewRepository.findHelpTypesByReviewIds(List.of(first.getId(), second.getId()));

            // then
            assertThat(result.get(first.getId()))
                    .containsExactlyInAnyOrder(ReviewHelpType.KIND_AND_WARM, ReviewHelpType.QUICK_RESPONSE);
            assertThat(result.get(second.getId())).containsExactly(ReviewHelpType.SAFE_STORAGE);
        }
    }
}
