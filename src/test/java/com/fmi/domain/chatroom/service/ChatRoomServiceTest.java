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
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostImageService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.userblock.service.BlockService;
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
}
