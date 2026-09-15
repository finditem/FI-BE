package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.service.internal.AuthEmailNotifier;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.mail.EmailBounceRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "member@finditem.kr";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AuthEmailNotifier authEmailNotifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailBounceRegistry emailBounceRegistry;

    @Captor
    private ArgumentCaptor<String> redisValueCaptor;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("인증 코드 발송")
    class SendCode {

        @Nested
        @DisplayName("가입 가능한 이메일이면")
        class WithAvailableEmail {

            @Test
            @DisplayName("기존 코드를 삭제한 뒤 5분 TTL로 저장하고 메일 발송을 요청한다")
            void 이메일_인증_코드_발송은_기존_코드를_삭제한_뒤_5분_TTL로_저장하고_메일을_요청한다() {
                // given
                when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
                when(emailBounceRegistry.hasBounced(EMAIL)).thenReturn(false);

                // when
                emailVerificationService.sendCode(EMAIL);

                // then
                verify(redis).delete("email:verify:" + EMAIL);
                verify(valueOperations)
                        .set(eq("email:verify:" + EMAIL), redisValueCaptor.capture(), eq(Duration.ofMinutes(5)));
                String[] codeAndExpiry = redisValueCaptor.getValue().split(":");
                verify(authEmailNotifier).sendVerificationCode(eq(EMAIL), eq(codeAndExpiry[0]));
                assertThat(codeAndExpiry).hasSize(2);
                assertThat(codeAndExpiry[0]).matches("\\d{6}");
                assertThat(Long.parseLong(codeAndExpiry[1]))
                        .isGreaterThan(Instant.now().getEpochSecond());
            }
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    class Verify {

        @Nested
        @DisplayName("유효한 인증 코드가 있으면")
        class WithValidCode {

            @Test
            @DisplayName("기존 코드를 삭제하고 24시간 인증 정보를 저장한다")
            void 유효한_이메일_인증_코드는_기존_코드를_삭제하고_24시간_인증_flag를_저장한다() {
                // given
                User user = User.builder().email(EMAIL).email_verified(false).build();
                String verifiedKey = "email:verified:" + EMAIL;
                when(valueOperations.get("email:verify:" + EMAIL))
                        .thenReturn("123456:"
                                + Instant.now().plus(Duration.ofMinutes(1)).getEpochSecond());
                when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

                // when
                emailVerificationService.verify(EMAIL, "123456");

                // then
                verify(redis).delete("email:verify:" + EMAIL);
                verify(valueOperations).set(verifiedKey, "true", Duration.ofHours(24));
                assertThat(user.isEmail_verified()).isTrue();
            }
        }
    }
}
