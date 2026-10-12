package com.fmi.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.exception.ReviewErrorStatus;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.userblock.data.BlockedUser;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("ReviewService")
class ReviewServiceTest extends IntegrationTestSupport {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FindCompletionRepository findCompletionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockedUserRepository blockedUserRepository;

    @Nested
    @DisplayName("후기를 작성할 때")
    class DescribeWrite {

        @Nested
        @DisplayName("찾기 완료를 처리한 요청자가 작성하면")
        class ContextWithRequester {

            @Test
            @DisplayName("도움을 준 사용자에게 받는 후기로 저장하고 상세 후기의 앞뒤 공백을 제거한다")
            void itSavesReviewForHelper() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("write-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("write-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910001L)
                        .requester(author)
                        .helper(helper)
                        .build());

                // when
                Review result = reviewService.write(
                        findCompletion.getId(),
                        author.getEmail(),
                        ReviewEmotion.MOVED,
                        Set.of(ReviewHelpType.KIND_AND_WARM, ReviewHelpType.QUICK_RESPONSE),
                        "  정말 감사합니다  ");

                // then
                Review saved = reviewRepository.findById(result.getId()).orElseThrow();
                assertThat(saved.isWrittenBy(author.getId())).isTrue();
                assertThat(saved.isReceivedBy(helper.getId())).isTrue();
                assertThat(saved.getEmotion()).isEqualTo(ReviewEmotion.MOVED);
                assertThat(saved.getHelpTypes())
                        .containsExactlyInAnyOrder(ReviewHelpType.KIND_AND_WARM, ReviewHelpType.QUICK_RESPONSE);
                assertThat(saved.getContent()).isEqualTo("정말 감사합니다");
            }
        }

        @Nested
        @DisplayName("상세 후기가 공백뿐이면")
        class ContextWithBlankContent {

            @Test
            @DisplayName("상세 후기 없이 저장한다")
            void itSavesWithoutContent() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("blank-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("blank-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910002L)
                        .requester(author)
                        .helper(helper)
                        .build());

                // when
                Review result = reviewService.write(
                        findCompletion.getId(),
                        author.getEmail(),
                        ReviewEmotion.GRATEFUL,
                        Set.of(ReviewHelpType.KIND_AND_WARM),
                        "   ");

                // then
                assertThat(reviewRepository
                                .findById(result.getId())
                                .orElseThrow()
                                .getContent())
                        .isNull();
            }
        }

        @Nested
        @DisplayName("상세 후기가 300자이면")
        class ContextWithMaxLengthContent {

            @Test
            @DisplayName("이모지를 한 글자로 세어 저장한다")
            void itSavesContentCountingCodePoints() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("max-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("max-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910003L)
                        .requester(author)
                        .helper(helper)
                        .build());
                String content = "😀".repeat(300);

                // when
                Review result = reviewService.write(
                        findCompletion.getId(),
                        author.getEmail(),
                        ReviewEmotion.GRATEFUL,
                        Set.of(ReviewHelpType.KIND_AND_WARM),
                        content);

                // then
                assertThat(reviewRepository
                                .findById(result.getId())
                                .orElseThrow()
                                .getContent())
                        .isEqualTo(content);
            }
        }

        @Nested
        @DisplayName("상세 후기가 300자를 넘으면")
        class ContextWithTooLongContent {

            @Test
            @DisplayName("INVALID_CONTENT 예외를 던진다")
            void itThrowsInvalidContent() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("long-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("long-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910004L)
                        .requester(author)
                        .helper(helper)
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                findCompletion.getId(),
                                author.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                "가".repeat(301)))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.INVALID_CONTENT);
            }
        }

        @Nested
        @DisplayName("찾기 완료를 처리한 요청자가 아니면")
        class ContextWithNonRequester {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void itThrowsAccessDenied() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("denied-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("denied-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910005L)
                        .requester(author)
                        .helper(helper)
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                findCompletion.getId(),
                                helper.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                "감사합니다"))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.ACCESS_DENIED);
            }
        }

        @Nested
        @DisplayName("이미 후기를 작성했으면")
        class ContextWithWrittenReview {

            @Test
            @DisplayName("ALREADY_WRITTEN 예외를 던진다")
            void itThrowsAlreadyWritten() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("written-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("written-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910006L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                findCompletion.getId(),
                                author.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                "감사합니다"))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.ALREADY_WRITTEN);
            }
        }

        @Nested
        @DisplayName("찾기 완료 기록이 없으면")
        class ContextWithoutCompletion {

            @Test
            @DisplayName("찾기 완료 NOT_FOUND 예외를 던진다")
            void itThrowsFindCompletionNotFound() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("기록없는작성자")
                        .email("missing-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                Long.MAX_VALUE,
                                author.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                null))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(FindCompletionErrorStatus.NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("받을 사용자가 탈퇴했으면")
        class ContextWithWithdrawnRecipient {

            @Test
            @DisplayName("NOT_ALLOWED 예외를 던진다")
            void itThrowsNotAllowed() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기탈퇴작성자")
                        .email("후기탈퇴-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기탈퇴도움자")
                        .email("후기탈퇴-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .deletedAt(LocalDateTime.of(2026, 9, 11, 10, 0))
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910008L)
                        .requester(author)
                        .helper(helper)
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                findCompletion.getId(),
                                author.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                null))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.NOT_ALLOWED);
            }
        }

        @Nested
        @DisplayName("받을 사용자가 나를 차단했으면")
        class ContextWithBlockedRecipient {

            @Test
            @DisplayName("NOT_ALLOWED 예외를 던진다")
            void itThrowsNotAllowed() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기차단작성자")
                        .email("후기차단-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기차단도움자")
                        .email("후기차단-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910009L)
                        .requester(author)
                        .helper(helper)
                        .build());
                blockedUserRepository.save(
                        BlockedUser.builder().blocker(helper).blocked(author).build());

                // when & then
                assertThatThrownBy(() -> reviewService.write(
                                findCompletion.getId(),
                                author.getEmail(),
                                ReviewEmotion.GRATEFUL,
                                Set.of(ReviewHelpType.KIND_AND_WARM),
                                null))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.NOT_ALLOWED);
            }
        }

        @Nested
        @DisplayName("같은 찾기 완료 건에 동시에 작성하면")
        class ContextWithConcurrentRequests {

            @Test
            @Transactional(propagation = Propagation.NOT_SUPPORTED)
            @DisplayName("한 요청만 저장하고 나머지는 ALREADY_WRITTEN 예외를 던진다")
            void itWritesOnlyOnce() throws Exception {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("concurrent-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("concurrent-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(910007L)
                        .requester(author)
                        .helper(helper)
                        .build());

                ExecutorService executor = Executors.newFixedThreadPool(2);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Review>> futures = new ArrayList<>();
                try {
                    for (int i = 0; i < 2; i++) {
                        Callable<Review> task = () -> {
                            start.await();
                            return reviewService.write(
                                    findCompletion.getId(),
                                    author.getEmail(),
                                    ReviewEmotion.GRATEFUL,
                                    Set.of(ReviewHelpType.KIND_AND_WARM),
                                    null);
                        };
                        futures.add(executor.submit(task));
                    }

                    // when
                    start.countDown();
                    int successCount = 0;
                    List<Throwable> failures = new ArrayList<>();
                    for (Future<Review> future : futures) {
                        try {
                            future.get();
                            successCount++;
                        } catch (ExecutionException e) {
                            failures.add(e.getCause());
                        }
                    }

                    // then
                    assertThat(successCount).isEqualTo(1);
                    assertThat(failures)
                            .singleElement()
                            .isInstanceOf(GeneralException.class)
                            .extracting("code")
                            .isEqualTo(ReviewErrorStatus.ALREADY_WRITTEN);
                    assertThat(reviewRepository.existsByFindCompletionId(findCompletion.getId()))
                            .isTrue();
                } finally {
                    executor.shutdownNow();
                    reviewRepository.deleteAll(reviewRepository.findAll().stream()
                            .filter(review -> review.isWrittenBy(author.getId()))
                            .toList());
                    findCompletionRepository.delete(findCompletion);
                    userRepository.deleteAll(List.of(author, helper));
                }
            }
        }
    }

    @Nested
    @DisplayName("후기를 숨길 때")
    class DescribeHide {

        @Nested
        @DisplayName("작성자가 숨기면")
        class ContextWithAuthor {

            @Test
            @DisplayName("작성자 목록에서만 숨김 처리한다")
            void itHidesOnlyForAuthor() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("hide-author-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("hide-author-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(920001L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when
                reviewService.hide(review.getId(), author.getEmail());

                // then
                Review hidden = reviewRepository.findById(review.getId()).orElseThrow();
                assertThat(hidden.isHiddenByAuthor()).isTrue();
                assertThat(hidden.isHiddenByRecipient()).isFalse();
            }
        }

        @Nested
        @DisplayName("받은 사용자가 숨기면")
        class ContextWithRecipient {

            @Test
            @DisplayName("받은 사용자 목록에서만 숨김 처리한다")
            void itHidesOnlyForRecipient() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("hide-recipient-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("hide-recipient-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(920002L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when
                reviewService.hide(review.getId(), helper.getEmail());

                // then
                Review hidden = reviewRepository.findById(review.getId()).orElseThrow();
                assertThat(hidden.isHiddenByRecipient()).isTrue();
                assertThat(hidden.isHiddenByAuthor()).isFalse();
            }
        }

        @Nested
        @DisplayName("이미 숨긴 후기를 다시 숨기면")
        class ContextWithHiddenReview {

            @Test
            @DisplayName("예외 없이 숨김 상태를 유지한다")
            void itKeepsHidden() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("hide-again-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("hide-again-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(920003L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());
                reviewService.hide(review.getId(), helper.getEmail());

                // when & then
                assertThatCode(() -> reviewService.hide(review.getId(), helper.getEmail()))
                        .doesNotThrowAnyException();
                assertThat(reviewRepository
                                .findById(review.getId())
                                .orElseThrow()
                                .isHiddenByRecipient())
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("작성자나 받은 사용자가 아니면")
        class ContextWithOtherUser {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void itThrowsAccessDenied() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("후기작성자")
                        .email("hide-other-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("후기도움자")
                        .email("hide-other-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User other = userRepository.save(User.builder()
                        .nickname("숨김제3자")
                        .email("hide-other-other@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(920004L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewService.hide(review.getId(), other.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.ACCESS_DENIED);
            }
        }
    }
}
