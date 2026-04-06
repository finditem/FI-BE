package com.fmi.domain.chatroom.converter;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.fmi.domain.chatroom.web.dto.ChatRoomResponseDTO.ChatRoomSummaryDTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ChatRoomConverterSummaryTest {

    private User contactUser;
    private ChatRoomParticipant participant;

    @BeforeEach
    void setUp() {
        contactUser = User.builder()
                .id(2L)
                .nickname("상대방")
                .email("other@test.com")
                .build();
    }

    @Test
    @DisplayName("toChatRoomSummaryDTO - 게시글 살아있을 때 FK 데이터를 사용한다")
    void toChatRoomSummaryDTO_postAlive_usesPostData() {
        Post post = mock(Post.class);
        given(post.getId()).willReturn(99L);
        given(post.getTitle()).willReturn("최신 제목");
        given(post.getPostType()).willReturn(PostType.LOST);
        given(post.getCategory()).willReturn(Category.WALLET);
        given(post.getAddress()).willReturn("서울특별시");
        given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
        given(post.isDeleted()).willReturn(false);

        ChatRoom room = mock(ChatRoom.class);
        given(room.getId()).willReturn(10L);
        given(room.isSource_post_deleted()).willReturn(false);
        given(room.getPost()).willReturn(post);

        participant = mock(ChatRoomParticipant.class);
        given(participant.getChatRoom()).willReturn(room);
        given(participant.getLastMessage()).willReturn(null);
        given(participant.getUnreadCount()).willReturn(0L);
        given(participant.getLastMessageSentAt()).willReturn(null);

        Map<Long, String> thumbnailMap = Map.of(99L, "https://s3.example.com/live-thumb.jpg");

        ChatRoomSummaryDTO result = ChatRoomConverter.toChatRoomSummaryDTO(participant, contactUser, thumbnailMap);

        assertThat(result.getPostInfo().getTitle()).isEqualTo("최신 제목");
        assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/live-thumb.jpg");
        assertThat(result.getPostInfo().isDeleted()).isFalse();
        assertThat(result.getPostInfo().getPostId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("toChatRoomSummaryDTO - 게시글 삭제 후 스냅샷 데이터를 사용한다")
    void toChatRoomSummaryDTO_postDeleted_usesSnapshotData() {
        ChatRoom room = mock(ChatRoom.class);
        given(room.getId()).willReturn(10L);
        given(room.isSource_post_deleted()).willReturn(true);
        given(room.getSource_post_id()).willReturn(99L);
        given(room.getSource_title()).willReturn("삭제 전 제목");
        given(room.getSource_post_type()).willReturn(PostType.LOST);
        given(room.getSource_category()).willReturn(Category.WALLET);
        given(room.getSource_address()).willReturn("서울특별시");
        given(room.getSource_thumbnail_url()).willReturn("https://s3.example.com/snapshot-thumb.jpg");
        given(room.getPostStatus()).willReturn(PostStatus.SEARCHING);

        participant = mock(ChatRoomParticipant.class);
        given(participant.getChatRoom()).willReturn(room);
        given(participant.getLastMessage()).willReturn(null);
        given(participant.getUnreadCount()).willReturn(0L);
        given(participant.getLastMessageSentAt()).willReturn(null);

        ChatRoomSummaryDTO result = ChatRoomConverter.toChatRoomSummaryDTO(participant, contactUser, Map.of());

        assertThat(result.getPostInfo().getTitle()).isEqualTo("삭제 전 제목");
        assertThat(result.getPostInfo().getThumbnailUrl()).isEqualTo("https://s3.example.com/snapshot-thumb.jpg");
        assertThat(result.getPostInfo().isDeleted()).isTrue();
        assertThat(result.getPostInfo().getPostId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("toChatRoomSummaryDTO - 삭제된 게시글은 thumbnailMap이 비어있어도 NPE가 나지 않는다")
    void toChatRoomSummaryDTO_postDeleted_emptyThumbnailMapNoNpe() {
        ChatRoom room = mock(ChatRoom.class);
        given(room.getId()).willReturn(10L);
        given(room.isSource_post_deleted()).willReturn(true);
        given(room.getSource_post_id()).willReturn(99L);
        given(room.getSource_title()).willReturn("제목");
        given(room.getSource_post_type()).willReturn(PostType.LOST);
        given(room.getSource_category()).willReturn(Category.WALLET);
        given(room.getSource_address()).willReturn("서울");
        given(room.getSource_thumbnail_url()).willReturn(null);
        given(room.getPostStatus()).willReturn(PostStatus.SEARCHING);

        participant = mock(ChatRoomParticipant.class);
        given(participant.getChatRoom()).willReturn(room);
        given(participant.getLastMessage()).willReturn(null);
        given(participant.getUnreadCount()).willReturn(0L);
        given(participant.getLastMessageSentAt()).willReturn(null);

        // NPE 없이 실행되어야 함
        ChatRoomSummaryDTO result = ChatRoomConverter.toChatRoomSummaryDTO(participant, contactUser, Map.of());

        assertThat(result).isNotNull();
        assertThat(result.getPostInfo().getThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("toChatRoomSummaryDTO - source_*에 stale한 SEARCHING이 있어도 FK로 최신 FOUND를 반환한다")
    void toChatRoomSummaryDTO_postStatusChanged_usesFkNotCache() {
        Post post = mock(Post.class);
        given(post.getId()).willReturn(99L);
        given(post.getTitle()).willReturn("지갑 찾았어요");
        given(post.getPostType()).willReturn(PostType.LOST);
        given(post.getCategory()).willReturn(Category.WALLET);
        given(post.getAddress()).willReturn("서울");
        given(post.getPostStatus()).willReturn(PostStatus.FOUND);

        ChatRoom room = mock(ChatRoom.class);
        given(room.getId()).willReturn(10L);
        given(room.isSource_post_deleted()).willReturn(false);
        given(room.getPost()).willReturn(post);
        given(room.getPostStatus()).willReturn(PostStatus.SEARCHING);

        participant = mock(ChatRoomParticipant.class);
        given(participant.getChatRoom()).willReturn(room);
        given(participant.getLastMessage()).willReturn(null);
        given(participant.getUnreadCount()).willReturn(0L);
        given(participant.getLastMessageSentAt()).willReturn(null);

        ChatRoomSummaryDTO result = ChatRoomConverter.toChatRoomSummaryDTO(participant, contactUser, Map.of(99L, "thumb.jpg"));

        assertThat(result.getPostInfo().getPostStatus()).isEqualTo(PostStatus.FOUND);
    }
}
