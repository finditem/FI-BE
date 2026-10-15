package com.fmi.domain.post.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class PostServiceDeleteTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PostFavoriteRepository postFavoriteRepository;

    @Mock
    private PostImageService postImageService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostStatisticsRecorder postStatisticsRecorder;

    @Spy
    private Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private PostService postService;

    private Post post;
    private UserDetails userDetails;
    private com.fmi.domain.user.data.User author;

    @BeforeEach
    void setUp() {
        author = com.fmi.domain.user.data.User.builder()
                .id(1L)
                .email("author@test.com")
                .nickname("작성자")
                .build();

        post = mock(Post.class);
        given(post.getUser()).willReturn(author);

        userDetails = mock(UserDetails.class);
        given(userDetails.getUsername()).willReturn("author@test.com");
    }

    @Test
    @DisplayName("deletePost - 연관된 모든 채팅방에 snapshotFromPost가 호출된다")
    void deletePost_snapshotsAllRelatedChatRooms() {
        ChatRoom room1 = mock(ChatRoom.class);
        ChatRoom room2 = mock(ChatRoom.class);
        String thumbnailUrl = "https://s3.example.com/thumb.jpg";

        given(userRepository.findActiveByEmailForUpdate("author@test.com")).willReturn(Optional.of(author));
        given(postRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(post));
        given(postImageService.findThumbnailImageUrl(post)).willReturn(thumbnailUrl);
        given(chatRoomRepository.findAllByPost(post)).willReturn(List.of(room1, room2));

        postService.deletePost(10L, userDetails);

        verify(room1).snapshotFromPost(post, thumbnailUrl);
        verify(room2).snapshotFromPost(post, thumbnailUrl);
    }

    @Test
    @DisplayName("deletePost - 썸네일 URL은 이미지 삭제 전에 먼저 캡처된다")
    void deletePost_thumbnailCapturedBeforeImageDeletion() {
        given(userRepository.findActiveByEmailForUpdate("author@test.com")).willReturn(Optional.of(author));
        given(postRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(post));
        given(postImageService.findThumbnailImageUrl(post)).willReturn("https://s3.example.com/thumb.jpg");
        given(chatRoomRepository.findAllByPost(post)).willReturn(List.of());

        postService.deletePost(10L, userDetails);

        InOrder inOrder = inOrder(postImageService);
        inOrder.verify(postImageService).findThumbnailImageUrl(post);
        inOrder.verify(postImageService).deleteAllImageByPost(post);
    }

    @Test
    @DisplayName("deletePost - 연관 채팅방이 없어도 정상 동작한다")
    void deletePost_noRelatedChatRooms_noError() {
        given(userRepository.findActiveByEmailForUpdate("author@test.com")).willReturn(Optional.of(author));
        given(postRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(post));
        given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
        given(chatRoomRepository.findAllByPost(post)).willReturn(List.of());

        postService.deletePost(10L, userDetails);

        verify(post).softDelete(any(LocalDateTime.class));
        verify(postImageService).deleteAllImageByPost(post);
    }

    @Test
    @DisplayName("deletePost - 썸네일이 없는 게시글도 채팅방 스냅샷이 정상 처리된다")
    void deletePost_noThumbnail_snapshotWithNullUrl() {
        ChatRoom room = mock(ChatRoom.class);

        given(userRepository.findActiveByEmailForUpdate("author@test.com")).willReturn(Optional.of(author));
        given(postRepository.findActiveByIdForUpdate(10L)).willReturn(Optional.of(post));
        given(postImageService.findThumbnailImageUrl(post)).willReturn(null);
        given(chatRoomRepository.findAllByPost(post)).willReturn(List.of(room));

        postService.deletePost(10L, userDetails);

        verify(room).snapshotFromPost(post, null);
    }
}
