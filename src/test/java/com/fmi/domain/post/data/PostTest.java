package com.fmi.domain.post.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.Category;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Post")
class PostTest {

    @Test
    @DisplayName("분실 게시글을 수정하면 유형은 유지하고 상태는 찾음으로 변경한다")
    void updateKeepsTypeAndChangesStatus() {
        LocalDateTime date = LocalDateTime.of(2026, 9, 1, 12, 0);
        Post post = Post.create(
                "잃어버린 지갑",
                "서울특별시 강남구",
                37.5,
                127.0,
                PostType.LOST,
                Category.WALLET,
                "지갑을 찾습니다",
                false,
                date,
                Radius.DISTANCE_1000,
                null);

        post.update("지갑을 찾았습니다", PostStatus.FOUND, date, null, null, null, null, null, null, null);

        assertThat(post.getPostType()).isEqualTo(PostType.LOST);
        assertThat(post.getPostStatus()).isEqualTo(PostStatus.FOUND);
        assertThat(post.getTitle()).isEqualTo("지갑을 찾았습니다");
    }
}
