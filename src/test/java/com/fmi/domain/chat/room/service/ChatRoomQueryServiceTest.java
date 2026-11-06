package com.fmi.domain.chat.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fmi.domain.chat.message.repository.ChatMessageRepository;
import com.fmi.domain.chat.room.data.ChatRoom;
import com.fmi.domain.chat.room.data.ChatRoomParticipant;
import com.fmi.domain.chat.room.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chat.room.repository.ChatRoomRepository;
import com.fmi.domain.chat.room.web.dto.response.ChatRoomResponse.ChatRoomResultResponse;
import com.fmi.domain.post.findcompletion.data.FindCompletion;
import com.fmi.domain.post.findcompletion.data.enums.FindCompletionStatus;
import com.fmi.domain.post.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.service.PostImageService;
import com.fmi.domain.post.review.repository.ReviewRepository;
import com.fmi.domain.user.block.service.BlockService;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatRoomQueryService")
class ChatRoomQueryServiceTest {
    @Nested
    @DisplayName("차단과 찾기 완료 정보를 조회할 때")
    class DescribeCompletion {

        @Mock
        private ChatRoomRepository chatRoomRepository;

        @Mock
        private PostRepository postRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private ChatRoomParticipantRepository chatRoomParticipantRepository;

        @Mock
        private ChatMessageRepository chatMessageRepository;

        @Mock
        private PostImageService postImageService;

        @Mock
        private BlockService blockService;

        @Mock
        private FindCompletionRepository findCompletionRepository;

        @Mock
        private ReviewRepository reviewRepository;

        @InjectMocks
        private ChatRoomQueryService chatRoomService;

        private User currentUser;
        private User opponent;
        private ChatRoom chatRoom;
        private Post post;
        private ChatRoomParticipant participant;

        @BeforeEach
        void setUp() {
            currentUser =
                    User.builder().id(1L).email("me@test.com").nickname("나").build();
            opponent =
                    User.builder().id(2L).email("other@test.com").nickname("상대").build();

            post = mock(Post.class);
            given(post.getId()).willReturn(99L);

            chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);

            participant = ChatRoomParticipant.builder()
                    .user(currentUser)
                    .unreadCount(0L)
                    .build();
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
        }

        @Test
        @DisplayName("getChatRoomDetail - 차단 상태이면 opponentUser.blocked = true")
        void getChatRoomDetail_blocked_returnsIsBlockedTrue() {
            // given
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(true);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getOpponentUser().isBlocked()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - 차단 상태가 아니면 opponentUser.blocked = false")
        void getChatRoomDetail_notBlocked_returnsIsBlockedFalse() {
            // given
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getOpponentUser().isBlocked()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 게시글의 찾기 완료 기록이 없으면 findCompletion.status = NONE")
        void getChatRoomDetail_notCompleted_returnsNone() {
            // given
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.empty());

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().getStatus()).isEqualTo(FindCompletionStatus.NONE);
            assertThat(result.getFindCompletion().getFindCompletionId()).isNull();
        }

        @Test
        @DisplayName("getChatRoomDetail - 이 채팅방에서 찾기 완료했으면 findCompletion.status = COMPLETED_HERE")
        void getChatRoomDetail_completedHere_returnsCompletedHere() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().getStatus()).isEqualTo(FindCompletionStatus.COMPLETED_HERE);
            assertThat(result.getFindCompletion().getFindCompletionId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("getChatRoomDetail - 같은 게시글의 다른 채팅방에서 찾기 완료했으면 findCompletion.status = COMPLETED_ELSEWHERE")
        void getChatRoomDetail_completedElsewhere_returnsCompletedElsewhere() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(false);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().getStatus()).isEqualTo(FindCompletionStatus.COMPLETED_ELSEWHERE);
            assertThat(result.getFindCompletion().getFindCompletionId()).isNull();
        }

        @Test
        @DisplayName("getChatRoomDetail - 이 채팅방에서 완료한 요청자가 후기를 쓰지 않았으면 canWriteReview = true")
        void getChatRoomDetail_requesterWithoutReview_canWriteReview() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(findCompletion.isRequestedBy(currentUser.getId())).willReturn(true);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));
            given(reviewRepository.existsByFindCompletionId(5L)).willReturn(false);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanWriteReview()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - 이미 후기를 작성했으면 canWriteReview = false")
        void getChatRoomDetail_reviewWritten_cannotWriteReview() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(findCompletion.isRequestedBy(currentUser.getId())).willReturn(true);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));
            given(reviewRepository.existsByFindCompletionId(5L)).willReturn(true);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanWriteReview()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 도움을 준 사용자가 조회하면 canWriteReview = false")
        void getChatRoomDetail_helper_cannotWriteReview() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(findCompletion.isRequestedBy(currentUser.getId())).willReturn(false);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanWriteReview()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 도움을 준 상대가 탈퇴했으면 canWriteReview = false")
        void getChatRoomDetail_withdrawnOpponent_cannotWriteReview() {
            // given
            User withdrawnOpponent = User.builder()
                    .id(2L)
                    .email("other@test.com")
                    .nickname("상대")
                    .deletedAt(LocalDateTime.of(2026, 9, 11, 10, 0))
                    .build();
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(withdrawnOpponent);
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(findCompletion.isRequestedBy(currentUser.getId())).willReturn(true);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanWriteReview()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 상대와 차단 관계이면 canWriteReview = false")
        void getChatRoomDetail_blockedOpponent_cannotWriteReview() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(findCompletion.isRequestedBy(currentUser.getId())).willReturn(true);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(true);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanWriteReview()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 분실물 게시글 작성자가 완료 전 채팅방을 조회하면 canComplete = true")
        void getChatRoomDetail_lostPostAuthor_canComplete() {
            // given
            given(post.getUser()).willReturn(currentUser);
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.empty());

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanComplete()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - 게시글 작성자가 아니면 canComplete = false")
        void getChatRoomDetail_notPostAuthor_cannotComplete() {
            // given
            given(post.getUser()).willReturn(opponent);
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.empty());

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanComplete()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 상대와 차단 관계이면 canComplete = false")
        void getChatRoomDetail_blockedOpponent_cannotComplete() {
            // given
            given(post.getUser()).willReturn(currentUser);
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.empty());
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(true);

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanComplete()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 이미 찾기 완료된 게시글이면 canComplete = false")
        void getChatRoomDetail_completedPost_cannotComplete() {
            // given
            FindCompletion findCompletion = mock(FindCompletion.class);
            given(findCompletion.getId()).willReturn(5L);
            given(findCompletion.isCompletedIn(10L)).willReturn(true);
            given(post.getUser()).willReturn(currentUser);
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getPostStatus()).willReturn(PostStatus.FOUND);
            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(findCompletionRepository.findBySourcePostId(99L)).willReturn(Optional.of(findCompletion));

            // when
            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            // then
            assertThat(result.getFindCompletion().isCanComplete()).isFalse();
        }
    }

    @Nested
    @DisplayName("게시글과 상대방 상태에 맞춰 상세를 조회할 때")
    class DescribeDetail {

        @Mock
        private ChatRoomRepository chatRoomRepository;

        @Mock
        private PostRepository postRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private ChatRoomParticipantRepository chatRoomParticipantRepository;

        @Mock
        private ChatMessageRepository chatMessageRepository;

        @Mock
        private PostImageService postImageService;

        @Mock
        private BlockService blockService;

        @Mock
        private FindCompletionRepository findCompletionRepository;

        @Mock
        private ReviewRepository reviewRepository;

        @InjectMocks
        private ChatRoomQueryService chatRoomService;

        private User currentUser;
        private User opponent;
        private ChatRoomParticipant participant;

        @BeforeEach
        void setUp() {
            currentUser =
                    User.builder().id(1L).email("me@test.com").nickname("나").build();
            opponent =
                    User.builder().id(2L).email("other@test.com").nickname("상대").build();

            participant = ChatRoomParticipant.builder()
                    .user(currentUser)
                    .unreadCount(3L)
                    .build();
        }

        @Test
        @DisplayName("getChatRoomDetail - 게시글 살아있을 때 FK로 최신 제목을 반환한다")
        void getChatRoomDetail_postAlive_returnsFkTitle() {
            Post post = mock(Post.class);
            given(post.getId()).willReturn(99L);
            given(post.getTitle()).willReturn("최신 제목");
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getCategory()).willReturn(Category.WALLET);
            given(post.getAddress()).willReturn("서울");
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(post.isDeleted()).willReturn(false);

            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(false);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(postImageService.findThumbnailImageUrl(post)).willReturn("https://s3.example.com/new-thumb.jpg");
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getPostInfo().getTitle()).isEqualTo("최신 제목");
            assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/new-thumb.jpg");
            assertThat(result.getPostInfo().isDeleted()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 게시글 소프트딜리트 후 스냅샷 제목을 반환한다")
        void getChatRoomDetail_postSoftDeleted_returnsSnapshotTitle() {
            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(true);
            given(chatRoom.getSourcePostId()).willReturn(99L);
            given(chatRoom.getSourceTitle()).willReturn("삭제 전 제목");
            given(chatRoom.getSourcePostType()).willReturn(PostType.LOST);
            given(chatRoom.getSourceCategory()).willReturn(Category.WALLET);
            given(chatRoom.getSourceAddress()).willReturn("서울");
            given(chatRoom.getSourceThumbnailUrl()).willReturn("https://s3.example.com/old-thumb.jpg");
            given(chatRoom.getSourcePostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getPostInfo().getTitle()).isEqualTo("삭제 전 제목");
            assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/old-thumb.jpg");
            assertThat(result.getPostInfo().isDeleted()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - 게시글 소프트딜리트 후 PostImageService를 호출하지 않는다")
        void getChatRoomDetail_postSoftDeleted_doesNotQueryPostImageService() {
            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(true);
            given(chatRoom.getSourcePostId()).willReturn(99L);
            given(chatRoom.getSourceTitle()).willReturn("삭제 전 제목");
            given(chatRoom.getSourcePostType()).willReturn(PostType.LOST);
            given(chatRoom.getSourceCategory()).willReturn(Category.WALLET);
            given(chatRoom.getSourceAddress()).willReturn("서울");
            given(chatRoom.getSourceThumbnailUrl()).willReturn(null);
            given(chatRoom.getSourcePostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            chatRoomService.getChatRoomDetail(10L, currentUser);

            org.mockito.Mockito.verify(postImageService, org.mockito.Mockito.never())
                    .findThumbnailImageUrl(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("getChatRoomDetail - source_*에 초기값이 있어도 FK로 최신값을 반환한다")
        void getChatRoomDetail_postStatusChanged_usesFkNotCache() {
            Post post = mock(Post.class);
            given(post.getId()).willReturn(99L);
            given(post.getTitle()).willReturn("지갑 찾았어요");
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getCategory()).willReturn(Category.WALLET);
            given(post.getAddress()).willReturn("서울");
            given(post.getPostStatus()).willReturn(PostStatus.FOUND); // FK 최신값
            given(post.isDeleted()).willReturn(false);

            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(false);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getSourcePostStatus()).willReturn(PostStatus.SEARCHING); // source_* 초기값
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getPostInfo().getPostStatus()).isEqualTo(PostStatus.FOUND);
        }

        @Test
        @DisplayName("getChatRoomDetail - 하드딜리트 후(post=null) 스냅샷을 반환한다")
        void getChatRoomDetail_postHardDeleted_returnsSnapshot() {
            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(true); // 소프트딜리트 때 이미 true
            given(chatRoom.getPost()).willReturn(null); // 하드딜리트 후 null
            given(chatRoom.getSourcePostId()).willReturn(99L);
            given(chatRoom.getSourceTitle()).willReturn("삭제된 게시글");
            given(chatRoom.getSourcePostType()).willReturn(PostType.LOST);
            given(chatRoom.getSourceCategory()).willReturn(Category.WALLET);
            given(chatRoom.getSourceAddress()).willReturn("서울");
            given(chatRoom.getSourceThumbnailUrl()).willReturn(null);
            given(chatRoom.getSourcePostStatus()).willReturn(PostStatus.SEARCHING);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getPostInfo().getTitle()).isEqualTo("삭제된 게시글");
            assertThat(result.getPostInfo().isDeleted()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - 상대가 탈퇴한 경우 withdrawn=true, 개인정보는 마스킹된다")
        void getChatRoomDetail_opponentWithdrawn_returnsMaskedDto() {
            User withdrawnOpponent = User.builder()
                    .id(2L)
                    .email("other@test.com")
                    .nickname("상대")
                    .profile_img("https://s3.example.com/p.jpg")
                    .email_verified(true)
                    .deletedAt(LocalDateTime.now().minusDays(1))
                    .build();

            Post post = mock(Post.class);
            given(post.getId()).willReturn(99L);
            given(post.getTitle()).willReturn("제목");
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getCategory()).willReturn(Category.WALLET);
            given(post.getAddress()).willReturn("서울");
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(post.isDeleted()).willReturn(false);

            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(false);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(withdrawnOpponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
            given(blockService.isBlocked(currentUser.getId(), withdrawnOpponent.getId()))
                    .willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getOpponentUser().isWithdrawn()).isTrue();
            assertThat(result.getOpponentUser().getOpponentUserId()).isEqualTo(2L);
            assertThat(result.getOpponentUser().getNickname()).isNull();
            assertThat(result.getOpponentUser().getProfileImageUrl()).isNull();
            assertThat(result.getOpponentUser().isEmailVerified()).isFalse();
            assertThat(result.getOpponentUser().isBlocked()).isFalse();
        }

        @Test
        @DisplayName("getChatRoomDetail - 상대가 활성 상태면 withdrawn=false, 기존 정보 그대로 반환한다")
        void getChatRoomDetail_opponentActive_returnsOriginalDto() {
            User activeOpponent = User.builder()
                    .id(2L)
                    .email("other@test.com")
                    .nickname("상대")
                    .profile_img("https://s3.example.com/p.jpg")
                    .email_verified(true)
                    .build();

            Post post = mock(Post.class);
            given(post.getId()).willReturn(99L);
            given(post.getTitle()).willReturn("제목");
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getCategory()).willReturn(Category.WALLET);
            given(post.getAddress()).willReturn("서울");
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(post.isDeleted()).willReturn(false);

            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(false);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(activeOpponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
            given(blockService.isBlocked(currentUser.getId(), activeOpponent.getId()))
                    .willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getOpponentUser().isWithdrawn()).isFalse();
            assertThat(result.getOpponentUser().getNickname()).isEqualTo("상대");
            assertThat(result.getOpponentUser().getProfileImageUrl()).isEqualTo("https://s3.example.com/p.jpg");
            assertThat(result.getOpponentUser().isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("getChatRoomDetail - unreadCount가 응답에 포함된다")
        void getChatRoomDetail_returnsUnreadCount() {
            Post post = mock(Post.class);
            given(post.getId()).willReturn(99L);
            given(post.getTitle()).willReturn("제목");
            given(post.getPostType()).willReturn(PostType.LOST);
            given(post.getCategory()).willReturn(Category.WALLET);
            given(post.getAddress()).willReturn("서울");
            given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
            given(post.isDeleted()).willReturn(false);

            ChatRoom chatRoom = mock(ChatRoom.class);
            given(chatRoom.getId()).willReturn(10L);
            given(chatRoom.isSourcePostDeleted()).willReturn(false);
            given(chatRoom.getPost()).willReturn(post);
            given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
            given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant); // unreadCount=3

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
            given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
            given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

            ChatRoomResultResponse result = chatRoomService.getChatRoomDetail(10L, currentUser);

            assertThat(result.getUnreadCount()).isEqualTo(3L);
        }
    }
}
