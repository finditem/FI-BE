package com.fmi.domain.auth.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationStore")
class EmailVerificationStoreTest {

    private static final String EMAIL = "member@finditem.kr";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EmailVerificationStore store;

    @BeforeEach
    void setUp() {
        store = new EmailVerificationStore(redis);
        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("인증 코드를 교체할 때")
    class ReplaceCode {

        @Test
        @DisplayName("기존 코드를 삭제하고 새 코드를 5분 동안 저장한다")
        void deletesExistingCodeAndStoresNewCodeForFiveMinutes() {
            Instant expiresAt = Instant.parse("2026-09-16T03:05:00Z");

            store.replaceCode(EMAIL, "123456", expiresAt);

            String key = "email:verify:" + EMAIL;
            long expiresAtEpochSecond = expiresAt.getEpochSecond();
            String storedValue = "123456:" + expiresAtEpochSecond;
            verify(redis).delete(key);
            verify(valueOperations).set(key, storedValue, Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("인증 코드를 조회할 때")
    class FindCode {

        @Test
        @DisplayName("저장 형식이 올바르면 코드와 만료 시각을 반환한다")
        void returnsStoredCodeAndExpiry() {
            Instant expiresAt = Instant.parse("2026-09-16T03:05:00Z");
            long expiresAtEpochSecond = expiresAt.getEpochSecond();
            String storedValue = "123456:" + expiresAtEpochSecond;
            when(valueOperations.get("email:verify:" + EMAIL)).thenReturn(storedValue);

            var result = store.findCode(EMAIL);

            var expected = new EmailVerificationStore.VerificationCode("123456", expiresAt);
            assertThat(result).contains(expected);
        }

        @Test
        @DisplayName("저장 형식이 잘못되면 값을 삭제하고 빈 결과를 반환한다")
        void deletesMalformedValueAndReturnsEmpty() {
            String key = "email:verify:" + EMAIL;
            when(valueOperations.get(key)).thenReturn("malformed");

            var result = store.findCode(EMAIL);

            assertThat(result).isEmpty();
            verify(redis).delete(key);
        }
    }

    @Test
    @DisplayName("이메일 인증 완료 상태를 24시간 동안 저장한다")
    void storesVerifiedStateForTwentyFourHours() {
        store.markVerified(EMAIL);

        verify(valueOperations).set("email:verified:" + EMAIL, "true", Duration.ofHours(24));
    }
}
