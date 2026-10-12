package com.fmi.domain.review.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewSummary")
class ReviewSummaryTest {

    @Nested
    @DisplayName("상대방의 탈퇴 여부를 판단할 때")
    class DescribeIsUserWithdrawn {

        @Nested
        @DisplayName("상대방이 활성 사용자이면")
        class ContextWithActiveUser {

            @Test
            @DisplayName("탈퇴하지 않은 것으로 판단한다")
            void itReturnsFalse() {
                ReviewSummary summary = new ReviewSummary(
                        1L, ReviewEmotion.GRATEFUL, null, LocalDateTime.of(2026, 9, 1, 10, 0), 2L, "닉네임", null, null);

                assertThat(summary.isUserWithdrawn()).isFalse();
            }
        }

        @Nested
        @DisplayName("상대방에게 탈퇴 일시가 있으면")
        class ContextWithSoftDeletedUser {

            @Test
            @DisplayName("탈퇴한 것으로 판단한다")
            void itReturnsTrue() {
                ReviewSummary summary = new ReviewSummary(
                        1L,
                        ReviewEmotion.GRATEFUL,
                        null,
                        LocalDateTime.of(2026, 9, 1, 10, 0),
                        2L,
                        "닉네임",
                        null,
                        LocalDateTime.of(2026, 9, 2, 10, 0));

                assertThat(summary.isUserWithdrawn()).isTrue();
            }
        }

        @Nested
        @DisplayName("상대방이 하드 삭제되어 없으면")
        class ContextWithHardDeletedUser {

            @Test
            @DisplayName("탈퇴한 것으로 판단한다")
            void itReturnsTrue() {
                ReviewSummary summary = new ReviewSummary(
                        1L, ReviewEmotion.GRATEFUL, null, LocalDateTime.of(2026, 9, 1, 10, 0), null, null, null, null);

                assertThat(summary.isUserWithdrawn()).isTrue();
            }
        }
    }
}
