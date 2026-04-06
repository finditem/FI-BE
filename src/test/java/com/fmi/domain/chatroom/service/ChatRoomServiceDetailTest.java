package com.fmi.domain.chatroom.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.chatmessage.repository.ChatMessageRepository;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomResultDTO;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostImageService;
import com.fmi.domain.userblock.service.BlockService;
import com.fmi.domain.Enum.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceDetailTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomParticipantRepository chatRoomParticipantRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private PostImageService postImageService;
    @Mock private BlockService blockService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    private User currentUser;
    private User opponent;
    private ChatRoomParticipant participant;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).email("me@test.com").nickname("나").build();
        opponent = User.builder().id(2L).email("other@test.com").nickname("상대").build();

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
        given(chatRoom.isSource_post_deleted()).willReturn(false);
        given(chatRoom.getPost()).willReturn(post);
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
        given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
        given(postImageService.findThumbnailImageUrl(post)).willReturn("https://s3.example.com/new-thumb.jpg");
        given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        assertThat(result.getPostInfo().getTitle()).isEqualTo("최신 제목");
        assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/new-thumb.jpg");
        assertThat(result.getPostInfo().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("getChatRoomDetail - 게시글 소프트딜리트 후 스냅샷 제목을 반환한다")
    void getChatRoomDetail_postSoftDeleted_returnsSnapshotTitle() {
        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(10L);
        given(chatRoom.isSource_post_deleted()).willReturn(true);
        given(chatRoom.getSource_post_id()).willReturn(99L);
        given(chatRoom.getSource_title()).willReturn("삭제 전 제목");
        given(chatRoom.getSource_post_type()).willReturn(PostType.LOST);
        given(chatRoom.getSource_category()).willReturn(Category.WALLET);
        given(chatRoom.getSource_address()).willReturn("서울");
        given(chatRoom.getSource_thumbnail_url()).willReturn("https://s3.example.com/old-thumb.jpg");
        given(chatRoom.getPostStatus()).willReturn(PostStatus.SEARCHING);
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
        given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
        given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        assertThat(result.getPostInfo().getTitle()).isEqualTo("삭제 전 제목");
        assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/old-thumb.jpg");
        assertThat(result.getPostInfo().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("getChatRoomDetail - 게시글 소프트딜리트 후 PostImageService를 호출하지 않는다")
    void getChatRoomDetail_postSoftDeleted_doesNotQueryPostImageService() {
        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(10L);
        given(chatRoom.isSource_post_deleted()).willReturn(true);
        given(chatRoom.getSource_post_id()).willReturn(99L);
        given(chatRoom.getSource_title()).willReturn("삭제 전 제목");
        given(chatRoom.getSource_post_type()).willReturn(PostType.LOST);
        given(chatRoom.getSource_category()).willReturn(Category.WALLET);
        given(chatRoom.getSource_address()).willReturn("서울");
        given(chatRoom.getSource_thumbnail_url()).willReturn(null);
        given(chatRoom.getPostStatus()).willReturn(PostStatus.SEARCHING);
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
        given(chatRoom.isSource_post_deleted()).willReturn(false);
        given(chatRoom.getPost()).willReturn(post);
        given(chatRoom.getPostStatus()).willReturn(PostStatus.SEARCHING); // source_* 초기값
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
        given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
        given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
        given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        assertThat(result.getPostInfo().getPostStatus()).isEqualTo(PostStatus.FOUND);
    }

    @Test
    @DisplayName("getChatRoomDetail - 하드딜리트 후(post=null) 스냅샷을 반환한다")
    void getChatRoomDetail_postHardDeleted_returnsSnapshot() {
        ChatRoom chatRoom = mock(ChatRoom.class);
        given(chatRoom.getId()).willReturn(10L);
        given(chatRoom.isSource_post_deleted()).willReturn(true); // 소프트딜리트 때 이미 true
        given(chatRoom.getPost()).willReturn(null); // 하드딜리트 후 null
        given(chatRoom.getSource_post_id()).willReturn(99L);
        given(chatRoom.getSource_title()).willReturn("삭제된 게시글");
        given(chatRoom.getSource_post_type()).willReturn(PostType.LOST);
        given(chatRoom.getSource_category()).willReturn(Category.WALLET);
        given(chatRoom.getSource_address()).willReturn("서울");
        given(chatRoom.getSource_thumbnail_url()).willReturn(null);
        given(chatRoom.getPostStatus()).willReturn(PostStatus.SEARCHING);
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
        given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant);

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
        given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        assertThat(result.getPostInfo().getTitle()).isEqualTo("삭제된 게시글");
        assertThat(result.getPostInfo().isDeleted()).isTrue();
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
        given(chatRoom.isSource_post_deleted()).willReturn(false);
        given(chatRoom.getPost()).willReturn(post);
        given(chatRoom.getOtherParticipant(currentUser.getId())).willReturn(opponent);
        given(chatRoom.getParticipant(currentUser.getId())).willReturn(participant); // unreadCount=3

        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
        given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
        given(blockService.isBlocked(currentUser.getId(), opponent.getId())).willReturn(false);

        ChatRoomResultDTO result = chatRoomService.getChatRoomDetail(10L, currentUser);

        assertThat(result.getUnreadCount()).isEqualTo(3L);
    }
}
