package com.fmi.domain.post.converter.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.Radius;
import com.fmi.domain.post.web.dto.response.PostGetResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostConverter")
class PostConverterTest {

    @Test
    @DisplayName("상세 응답에 등록 일시와 분실 또는 습득 일시를 구분해 제공한다")
    void includesCreatedAtAndOccurredAt() {
        // given
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 13, 18, 20);
        Post post = Post.create(
                "분실물",
                "서울특별시 성동구",
                37.5,
                127.0,
                PostType.LOST,
                Category.ELECTRONICS,
                "휴대전화를 잃어버렸습니다.",
                false,
                occurredAt,
                Radius.DISTANCE_1000,
                null);

        // when
        PostGetResponse response = PostConverter.toGetResponse(post, false, 0, true, false, 0, true, List.of(), null);

        // then
        assertThat(response.createdAt()).isEqualTo(post.getCreatedAt());
        assertThat(response.date()).isEqualTo(occurredAt);
    }
}
