package com.fmi.domain.chatroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomResultDTO;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.data.FindCompletionStatus;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostImageService;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.userblock.service.BlockService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceTest {

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
    private ChatRoomService chatRoomService;

    private User currentUser;
    private User opponent;
    private ChatRoom chatRoom;
    private Post post;
    private ChatRoomParticipant participant;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).email("me@test.com").nickname("나").build();
        opponent = User.builder().id(2L).email("other@test.com").nickname("상대").build();

        post = mock(Post.class);
        given(post.getId()).willReturn(99L);

        chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(10L);
        given(chatRoom.getPost()).willReturn(post);
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);

        participant =
                ChatRoomParticipant.builder().user(currentUser).unreadCount(0L).build();
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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

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
        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        // then
        assertThat(result.getFindCompletion().isCanComplete()).isFalse();
    }
}
