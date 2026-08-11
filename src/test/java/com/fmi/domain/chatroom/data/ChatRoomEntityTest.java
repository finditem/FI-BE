package com.fmi.domain.chatroom.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ChatRoomEntityTest {

    private Post post;

    @BeforeEach
    void setUp() {
        post = mock(Post.class);
        given(post.getId()).willReturn(42L);
        given(post.getTitle()).willReturn("잃어버린 지갑");
        given(post.getAddress()).willReturn("서울특별시 강남구");
        given(post.getPostType()).willReturn(PostType.LOST);
        given(post.getCategory()).willReturn(Category.WALLET);
        given(post.getPostStatus()).willReturn(PostStatus.SEARCHING);
    }

    @Test
    @DisplayName("initFromPost - source_* 필드가 게시글 값으로 채워진다")
    void initFromPost_copiesAllFields() {
        ChatRoom chatRoom = ChatRoom.builder().createdAt(java.time.LocalDateTime.now()).build();

        chatRoom.initFromPost(post);

        assertThat(chatRoom.getSourcePostId()).isEqualTo(42L);
        assertThat(chatRoom.getSourceTitle()).isEqualTo("잃어버린 지갑");
        assertThat(chatRoom.getSourceAddress()).isEqualTo("서울특별시 강남구");
        assertThat(chatRoom.getSourcePostType()).isEqualTo(PostType.LOST);
        assertThat(chatRoom.getSourceCategory()).isEqualTo(Category.WALLET);
        assertThat(chatRoom.getSourcePostStatus()).isEqualTo(PostStatus.SEARCHING);
    }

    @Test
    @DisplayName("initFromPost - source_post_deleted는 false를 유지한다")
    void initFromPost_doesNotMarkAsDeleted() {
        ChatRoom chatRoom = ChatRoom.builder().createdAt(java.time.LocalDateTime.now()).build();

        chatRoom.initFromPost(post);

        assertThat(chatRoom.isSourcePostDeleted()).isFalse();
    }

    @Test
    @DisplayName("initFromPost - source_thumbnail_url은 설정하지 않는다")
    void initFromPost_doesNotSetThumbnail() {
        ChatRoom chatRoom = ChatRoom.builder().createdAt(java.time.LocalDateTime.now()).build();

        chatRoom.initFromPost(post);

        assertThat(chatRoom.getSourceThumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("snapshotFromPost - source_* 필드와 썸네일이 채워지고 deleted=true가 된다")
    void snapshotFromPost_copiesAllFieldsAndMarksDeleted() {
        ChatRoom chatRoom = ChatRoom.builder().createdAt(java.time.LocalDateTime.now()).build();

        chatRoom.snapshotFromPost(post, "https://s3.example.com/thumb.jpg");

        assertThat(chatRoom.getSourcePostId()).isEqualTo(42L);
        assertThat(chatRoom.getSourceTitle()).isEqualTo("잃어버린 지갑");
        assertThat(chatRoom.getSourceAddress()).isEqualTo("서울특별시 강남구");
        assertThat(chatRoom.getSourcePostType()).isEqualTo(PostType.LOST);
        assertThat(chatRoom.getSourceCategory()).isEqualTo(Category.WALLET);
        assertThat(chatRoom.getSourcePostStatus()).isEqualTo(PostStatus.SEARCHING);
        assertThat(chatRoom.getSourceThumbnailUrl()).isEqualTo("https://s3.example.com/thumb.jpg");
        assertThat(chatRoom.isSourcePostDeleted()).isTrue();
    }

    @Test
    @DisplayName("snapshotFromPost - 썸네일이 null이어도 정상 동작한다")
    void snapshotFromPost_nullThumbnail_doesNotThrow() {
        ChatRoom chatRoom = ChatRoom.builder().createdAt(java.time.LocalDateTime.now()).build();

        chatRoom.snapshotFromPost(post, null);

        assertThat(chatRoom.getSourceThumbnailUrl()).isNull();
        assertThat(chatRoom.isSourcePostDeleted()).isTrue();
    }

    @Test
    @DisplayName("clearPostReference - post FK가 null이 된다")
    void clearPostReference_setsPostToNull() {
        Post livePost = mock(Post.class);
        ChatRoom chatRoom = ChatRoom.builder()
                .post(livePost)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        chatRoom.clearPostReference();

        assertThat(chatRoom.getPost()).isNull();
    }
}
