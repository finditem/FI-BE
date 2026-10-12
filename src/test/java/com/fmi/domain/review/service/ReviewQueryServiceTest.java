package com.fmi.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.data.ReviewDetail;
import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewListType;
import com.fmi.domain.review.data.ReviewPage;
import com.fmi.domain.review.data.ReviewSummary;
import com.fmi.domain.review.exception.ReviewErrorStatus;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@DisplayName("ReviewQueryService")
class ReviewQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private ReviewQueryService reviewQueryService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FindCompletionRepository findCompletionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Nested
    @DisplayName("내 후기 목록을 조회할 때")
    class DescribeGetMyReceivedReviews {

        @Nested
        @DisplayName("요청 개수보다 후기가 많으면")
        class ContextWithMoreReviewsThanSize {

            @Test
            @DisplayName("요청 개수만 반환하고 다음 커서로 나머지를 이어서 조회한다")
            void itPagesWithCursor() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("페이지작성자")
                        .email("page-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("페이지수신자")
                        .email("page-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(940001L + i)
                            .requester(author)
                            .helper(recipient)
                            .build());
                    reviews.add(reviewRepository.save(Review.builder()
                            .findCompletion(findCompletion)
                            .emotion(ReviewEmotion.GRATEFUL)
                            .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                            .build()));
                }

                // when
                ReviewPage firstPage =
                        reviewQueryService.getMyReviews(recipient.getEmail(), ReviewListType.RECEIVED, null, 2);
                ReviewPage secondPage = reviewQueryService.getMyReviews(
                        recipient.getEmail(), ReviewListType.RECEIVED, firstPage.nextCursor(), 2);

                // then
                assertThat(firstPage.reviews())
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(2).getId(), reviews.get(1).getId());
                assertThat(firstPage.hasNext()).isTrue();
                assertThat(firstPage.nextCursor()).isEqualTo(reviews.get(1).getId());
                assertThat(firstPage.totalCount()).isEqualTo(3L);
                assertThat(firstPage.helpTypesByReviewId().get(reviews.get(2).getId()))
                        .containsExactly(ReviewHelpType.KIND_AND_WARM);
                assertThat(secondPage.reviews())
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(0).getId());
                assertThat(secondPage.hasNext()).isFalse();
                assertThat(secondPage.nextCursor()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("보낸 후기 탭을 조회하면")
    class DescribeGetMySentReviews {

        @Test
        @DisplayName("내가 작성한 후기를 받은 사용자 정보와 함께 반환한다")
        void itReturnsSentReviews() {
            // given
            User author = userRepository.save(User.builder()
                    .nickname("보낸탭작성자")
                    .email("sent-tab-author@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            User recipient = userRepository.save(User.builder()
                    .nickname("보낸탭수신자")
                    .email("sent-tab-recipient@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                    .sourcePostId(940201L)
                    .requester(author)
                    .helper(recipient)
                    .build());
            Review review = reviewRepository.save(Review.builder()
                    .findCompletion(findCompletion)
                    .emotion(ReviewEmotion.GRATEFUL)
                    .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                    .build());

            // when
            ReviewPage sentPage = reviewQueryService.getMyReviews(author.getEmail(), ReviewListType.SENT, null, 10);
            ReviewPage receivedPage =
                    reviewQueryService.getMyReviews(author.getEmail(), ReviewListType.RECEIVED, null, 10);

            // then
            assertThat(sentPage.reviews()).singleElement().satisfies(summary -> {
                assertThat(summary.reviewId()).isEqualTo(review.getId());
                assertThat(summary.userId()).isEqualTo(recipient.getId());
            });
            assertThat(sentPage.totalCount()).isEqualTo(1L);
            assertThat(receivedPage.reviews()).isEmpty();
        }
    }

    @Nested
    @DisplayName("받은 사용자가 숨긴 후기가 있으면")
    class DescribeHiddenByRecipient {

        @Test
        @DisplayName("받은 후기 탭에서는 빼고 타인 프로필에서는 개수 없이 보여준다")
        void itHidesOnlyFromRecipientOwnList() {
            // given
            User author = userRepository.save(User.builder()
                    .nickname("프로필작성자")
                    .email("profile-author@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            User recipient = userRepository.save(User.builder()
                    .nickname("프로필수신자")
                    .email("profile-recipient@test.com")
                    .password("password")
                    .role(Role.USER)
                    .build());
            List<Review> reviews = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(940101L + i)
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

            // when
            ReviewPage myPage =
                    reviewQueryService.getMyReviews(recipient.getEmail(), ReviewListType.RECEIVED, null, 10);
            ReviewPage profilePage = reviewQueryService.getProfileReceivedReviews(recipient.getId(), null, 10);

            // then
            assertThat(myPage.reviews())
                    .extracting(ReviewSummary::reviewId)
                    .containsExactly(reviews.get(1).getId());
            assertThat(myPage.totalCount()).isEqualTo(1L);
            assertThat(profilePage.reviews()).hasSize(2);
            assertThat(profilePage.totalCount()).isNull();
        }
    }

    @Nested
    @DisplayName("후기 상세를 조회할 때")
    class DescribeGetReview {

        @Nested
        @DisplayName("원본 게시글이 남아 있으면")
        class ContextWithActivePost {

            @Test
            @DisplayName("후기 내용과 보낸 사용자, 받은 사용자, 게시글 정보를 반환한다")
            void itReturnsReviewWithPost() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("상세작성자")
                        .email("상세-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("상세도움자")
                        .email("상세-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "지갑을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.WALLET,
                        "검은색 지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(post.getId())
                        .chatRoom(null)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM))
                        .content("감사합니다")
                        .build());

                // when
                ReviewDetail result = reviewQueryService.getReview(review.getId());

                // then
                assertThat(result.emotion()).isEqualTo(ReviewEmotion.MOVED);
                assertThat(result.helpTypes())
                        .containsExactlyInAnyOrder(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM);
                assertThat(result.content()).isEqualTo("감사합니다");
                assertThat(result.author().userId()).isEqualTo(author.getId());
                assertThat(result.recipient().userId()).isEqualTo(helper.getId());
                assertThat(result.post().postId()).isEqualTo(post.getId());
                assertThat(result.post().title()).isEqualTo("지갑을 잃어버렸어요");
                assertThat(result.post().address()).isEqualTo("서울 성동구");
                assertThat(result.post().date()).isEqualTo(LocalDateTime.of(2026, 9, 10, 10, 0));
                assertThat(result.post().createdAt()).isNotNull();
                assertThat(result.post().deleted()).isFalse();
            }
        }

        @Nested
        @DisplayName("원본 게시글이 삭제되었으면")
        class ContextWithDeletedPost {

            @Test
            @DisplayName("게시글 정보는 남은 게시글에서, 썸네일은 채팅방 스냅샷에서 반환한다")
            void itReturnsSnapshotThumbnail() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("삭제글작성자")
                        .email("삭제글-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("삭제글도움자")
                        .email("삭제글-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                Post post = postRepository.save(Post.create(
                        "지갑을 잃어버렸어요",
                        "서울 성동구",
                        37.5422,
                        127.0549,
                        PostType.LOST,
                        Category.WALLET,
                        "검은색 지갑입니다.",
                        false,
                        LocalDateTime.of(2026, 9, 10, 10, 0),
                        Radius.DISTANCE_1000,
                        author));
                ChatRoom chatRoom = ChatRoom.builder()
                        .post(post)
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build();
                chatRoom.initFromPost(post);
                chatRoom.snapshotFromPost(post, "https://cdn.test/snapshot-thumbnail.jpg");
                chatRoomRepository.save(chatRoom);
                post.softDelete();
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(post.getId())
                        .chatRoom(chatRoom)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM))
                        .content("감사합니다")
                        .build());

                // when
                ReviewDetail result = reviewQueryService.getReview(review.getId());

                // then
                assertThat(result.post().title()).isEqualTo("지갑을 잃어버렸어요");
                assertThat(result.post().date()).isEqualTo(LocalDateTime.of(2026, 9, 10, 10, 0));
                assertThat(result.post().thumbnailUrl()).isEqualTo("https://cdn.test/snapshot-thumbnail.jpg");
                assertThat(result.post().deleted()).isTrue();
            }
        }

        @Nested
        @DisplayName("원본 게시글 row가 없으면")
        class ContextWithoutPostRow {

            @Test
            @DisplayName("채팅방 스냅샷의 제목과 주소를 반환하고 날짜는 비운다")
            void itReturnsChatRoomSnapshot() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("스냅샷작성자")
                        .email("스냅샷-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("스냅샷도움자")
                        .email("스냅샷-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .sourceTitle("스냅샷 제목")
                        .sourceAddress("스냅샷 주소")
                        .sourceThumbnailUrl("https://cdn.test/snapshot.jpg")
                        .sourcePostDeleted(true)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(Long.MAX_VALUE)
                        .chatRoom(chatRoom)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM))
                        .content("감사합니다")
                        .build());

                // when
                ReviewDetail result = reviewQueryService.getReview(review.getId());

                // then
                assertThat(result.post().title()).isEqualTo("스냅샷 제목");
                assertThat(result.post().address()).isEqualTo("스냅샷 주소");
                assertThat(result.post().thumbnailUrl()).isEqualTo("https://cdn.test/snapshot.jpg");
                assertThat(result.post().date()).isNull();
                assertThat(result.post().createdAt()).isNull();
                assertThat(result.post().deleted()).isTrue();
            }
        }

        @Nested
        @DisplayName("받은 사용자가 숨긴 후기이면")
        class ContextWithHiddenReview {

            @Test
            @DisplayName("숨김과 관계없이 조회한다")
            void itReturnsHiddenReview() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("숨김상세작성자")
                        .email("숨김상세-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("숨김상세도움자")
                        .email("숨김상세-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(960001L)
                        .chatRoom(null)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM))
                        .content("감사합니다")
                        .build());
                review.hideByRecipient();

                // when
                ReviewDetail result = reviewQueryService.getReview(review.getId());

                // then
                assertThat(result.reviewId()).isEqualTo(review.getId());
            }
        }

        @Nested
        @DisplayName("삭제 상태인 후기이면")
        class ContextWithDeletedReview {

            @Test
            @DisplayName("NOT_FOUND 예외를 던진다")
            void itThrowsNotFound() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("삭제후기작성자")
                        .email("삭제후기-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("삭제후기도움자")
                        .email("삭제후기-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(960002L)
                        .chatRoom(null)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.MOVED)
                        .helpTypes(Set.of(ReviewHelpType.SAFE_STORAGE, ReviewHelpType.KIND_AND_WARM))
                        .content("감사합니다")
                        .build());
                review.delete(LocalDateTime.of(2026, 9, 11, 10, 0));

                // when & then
                assertThatThrownBy(() -> reviewQueryService.getReview(review.getId()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.NOT_FOUND);
            }
        }
    }
}
