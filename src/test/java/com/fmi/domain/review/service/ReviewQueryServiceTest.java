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
import org.springframework.transaction.annotation.Propagation;
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
    class DescribeGetMyReviews {

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

        @Nested
        @DisplayName("type이 SENT이면")
        class ContextWithSentType {

            @Test
            @DisplayName("작성한 후기만 받은 사용자 정보와 함께 반환한다")
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
        class ContextWithHiddenByRecipient {

            @Test
            @DisplayName("받은 후기 목록과 개수에서 뺀다")
            void itExcludesHiddenReviews() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("내목록작성자")
                        .email("my-hidden-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("내목록수신자")
                        .email("my-hidden-recipient@test.com")
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
                ReviewPage result =
                        reviewQueryService.getMyReviews(recipient.getEmail(), ReviewListType.RECEIVED, null, 10);

                // then
                assertThat(result.reviews())
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(1).getId());
                assertThat(result.totalCount()).isEqualTo(1L);
            }
        }
    }

    @Nested
    @DisplayName("타인 프로필의 받은 후기를 조회할 때")
    class DescribeGetProfileReceivedReviews {

        @Nested
        @DisplayName("받은 사용자가 숨긴 후기가 있으면")
        class ContextWithHiddenByRecipient {

            @Test
            @DisplayName("숨긴 후기도 개수 없이 반환한다")
            void itIncludesHiddenReviewsWithoutCount() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("프로필작성자")
                        .email("profile-hidden-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User recipient = userRepository.save(User.builder()
                        .nickname("프로필수신자")
                        .email("profile-hidden-recipient@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                List<Review> reviews = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                            .sourcePostId(940301L + i)
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
                ReviewPage result = reviewQueryService.getProfileReceivedReviews(recipient.getId(), null, 10);

                // then
                assertThat(result.reviews())
                        .extracting(ReviewSummary::reviewId)
                        .containsExactly(reviews.get(1).getId(), reviews.get(0).getId());
                assertThat(result.totalCount()).isNull();
            }
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
                ReviewDetail result = reviewQueryService.getReview(review.getId(), author.getEmail());

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
                ReviewDetail result = reviewQueryService.getReview(review.getId(), author.getEmail());

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
                ReviewDetail result = reviewQueryService.getReview(review.getId(), author.getEmail());

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
                ReviewDetail result = reviewQueryService.getReview(review.getId(), author.getEmail());

                // then
                assertThat(result.reviewId()).isEqualTo(review.getId());
            }
        }

        @Nested
        @DisplayName("후기를 주고받은 당사자가 조회하면")
        class ContextWithParticipantViewer {

            @Test
            @DisplayName("채팅방 이동용 roomId를 반환한다")
            void itReturnsChatRoomId() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("당사자작성자")
                        .email("당사자-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("당사자도움자")
                        .email("당사자-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User other = userRepository.save(User.builder()
                        .nickname("당사자제3자")
                        .email("당사자-other@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(960004L)
                        .chatRoom(chatRoom)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when
                ReviewDetail authorView = reviewQueryService.getReview(review.getId(), author.getEmail());
                ReviewDetail helperView = reviewQueryService.getReview(review.getId(), helper.getEmail());

                // then
                assertThat(authorView.roomId()).isEqualTo(chatRoom.getId());
                assertThat(helperView.roomId()).isEqualTo(chatRoom.getId());
            }
        }

        @Nested
        @DisplayName("후기를 주고받은 당사자가 아니면")
        class ContextWithNonParticipantViewer {

            @Test
            @DisplayName("ACCESS_DENIED 예외를 던진다")
            void itThrowsAccessDenied() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("제3자조회작성자")
                        .email("제3자조회-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("제3자조회도움자")
                        .email("제3자조회-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User other = userRepository.save(User.builder()
                        .nickname("제3자조회제3자")
                        .email("제3자조회-other@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                        .createdAt(LocalDateTime.of(2026, 9, 10, 11, 0))
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(960005L)
                        .chatRoom(chatRoom)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.KIND_AND_WARM))
                        .build());

                // when & then
                assertThatThrownBy(() -> reviewQueryService.getReview(review.getId(), other.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.ACCESS_DENIED);
            }
        }

        @Nested
        @DisplayName("조회가 끝난 뒤 트랜잭션 밖에서 결과를 읽으면")
        class ContextOutsideTransaction {

            @Test
            @Transactional(propagation = Propagation.NOT_SUPPORTED)
            @DisplayName("도움 경험을 지연 로딩 없이 읽을 수 있다")
            void itReadsHelpTypesWithoutSession() {
                // given
                User author = userRepository.save(User.builder()
                        .nickname("세션밖작성자")
                        .email("outside-tx-author@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                User helper = userRepository.save(User.builder()
                        .nickname("세션밖도움자")
                        .email("outside-tx-helper@test.com")
                        .password("password")
                        .role(Role.USER)
                        .build());
                FindCompletion findCompletion = findCompletionRepository.save(FindCompletion.builder()
                        .sourcePostId(960003L)
                        .requester(author)
                        .helper(helper)
                        .build());
                Review review = reviewRepository.save(Review.builder()
                        .findCompletion(findCompletion)
                        .emotion(ReviewEmotion.GRATEFUL)
                        .helpTypes(Set.of(ReviewHelpType.QUICK_RESPONSE, ReviewHelpType.SAFE_STORAGE))
                        .build());

                try {
                    // when
                    ReviewDetail result = reviewQueryService.getReview(review.getId(), author.getEmail());

                    // then
                    assertThat(result.helpTypes())
                            .containsExactlyInAnyOrder(ReviewHelpType.QUICK_RESPONSE, ReviewHelpType.SAFE_STORAGE);
                } finally {
                    reviewRepository.deleteById(review.getId());
                    findCompletionRepository.delete(findCompletion);
                    userRepository.deleteAll(List.of(author, helper));
                }
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
                assertThatThrownBy(() -> reviewQueryService.getReview(review.getId(), author.getEmail()))
                        .isInstanceOf(GeneralException.class)
                        .extracting("code")
                        .isEqualTo(ReviewErrorStatus.NOT_FOUND);
            }
        }
    }
}
