package com.fmi.domain.review.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ReviewUser")
class ReviewUserTest {

    @Nested
    @DisplayName("탈퇴 여부를 판단할 때")
    class DescribeIsWithdrawn {

        @Nested
        @DisplayName("활성 사용자이면")
        class ContextWithActiveUser {

            @Test
            @DisplayName("탈퇴하지 않은 것으로 판단한다")
            void itReturnsFalse() {
                ReviewUser user = new ReviewUser(2L, "닉네임", null, null);

                assertThat(user.isWithdrawn()).isFalse();
            }
        }

        @Nested
        @DisplayName("탈퇴 일시가 있으면")
        class ContextWithSoftDeletedUser {

            @Test
            @DisplayName("탈퇴한 것으로 판단한다")
            void itReturnsTrue() {
                ReviewUser user = new ReviewUser(2L, "닉네임", null, LocalDateTime.of(2026, 9, 2, 10, 0));

                assertThat(user.isWithdrawn()).isTrue();
            }
        }

        @Nested
        @DisplayName("하드 삭제되어 사용자 id가 없으면")
        class ContextWithHardDeletedUser {

            @Test
            @DisplayName("탈퇴한 것으로 판단한다")
            void itReturnsTrue() {
                ReviewUser user = new ReviewUser(null, null, null, null);

                assertThat(user.isWithdrawn()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("사용자 엔티티로 만들 때")
    class DescribeFrom {

        @Nested
        @DisplayName("사용자가 없으면")
        class ContextWithoutUser {

            @Test
            @DisplayName("탈퇴한 사용자로 만든다")
            void itCreatesWithdrawnUser() {
                ReviewUser user = ReviewUser.from(null);

                assertThat(user.userId()).isNull();
                assertThat(user.isWithdrawn()).isTrue();
            }
        }
    }
}
