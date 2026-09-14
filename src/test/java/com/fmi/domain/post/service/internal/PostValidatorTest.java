package com.fmi.domain.post.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PostValidator")
class PostValidatorTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 12, 0);

    private PostValidator postValidator;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(SEOUL_ZONE).toInstant(), SEOUL_ZONE);
        postValidator = new PostValidator(clock);
    }

    @Test
    @DisplayName("분실 또는 습득 일시가 미래이면 예외를 던진다")
    void rejectsFutureDate() {
        assertThatThrownBy(() -> postValidator.validateDate(NOW.plusNanos(1)))
                .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                        .isEqualTo(ErrorStatus._POST_DATE_IN_FUTURE));
    }

    @Test
    @DisplayName("분실 또는 습득 일시가 현재이면 허용한다")
    void acceptsCurrentDate() {
        assertThatCode(() -> postValidator.validateDate(NOW)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("수정 요청의 분실 또는 습득 일시가 없으면 허용한다")
    void acceptsNullDate() {
        assertThatCode(() -> postValidator.validateDate(null)).doesNotThrowAnyException();
    }
}
