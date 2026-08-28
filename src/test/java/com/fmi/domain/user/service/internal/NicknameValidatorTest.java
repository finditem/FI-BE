package com.fmi.domain.user.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NicknameValidator")
class NicknameValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NicknameValidator nicknameValidator;

    @BeforeEach
    void setUp() {
        nicknameValidator.init();
    }

    @Nested
    @DisplayName("닉네임을 검증할 때")
    class DescribeValidate {

        @Test
        @DisplayName("2~10자의 한글·영문·숫자 닉네임은 유효하다")
        void itAcceptsNicknameFollowingPolicy() {
            assertThat(nicknameValidator.validate("찾아줘토끼1").valid()).isTrue();
        }

        @Test
        @DisplayName("10자를 초과하면 유효하지 않다")
        void itRejectsNicknameLongerThanTenCharacters() {
            assertThat(nicknameValidator.validate("가나다라마바사아자차카").failure())
                    .isEqualTo(NicknameValidator.Failure.INVALID);
        }

        @Test
        @DisplayName("특수문자를 포함하면 유효하지 않다")
        void itRejectsSpecialCharacters() {
            assertThat(nicknameValidator.validate("찾아줘_토끼").failure()).isEqualTo(NicknameValidator.Failure.INVALID);
        }

        @Test
        @DisplayName("금칙어를 포함하면 유효하지 않다")
        void itRejectsBannedWords() {
            assertThat(nicknameValidator.validate("관리자토끼").failure()).isEqualTo(NicknameValidator.Failure.INVALID);
        }
    }

    @Nested
    @DisplayName("사용 가능한 닉네임을 검증할 때")
    class DescribeValidateAvailable {

        @Test
        @DisplayName("형식이 유효하지 않으면 중복을 조회하지 않는다")
        void itDoesNotCheckDuplicationForInvalidNickname() {
            nicknameValidator.validateAvailable("_");

            verify(userRepository, never()).existsByNickname("_");
        }

        @Test
        @DisplayName("활성 사용자가 같은 닉네임을 사용하면 중복이다")
        void itReturnsDuplicateForExistingNickname() {
            when(userRepository.existsByNickname("찾아줘토끼1")).thenReturn(true);

            NicknameValidator.ValidationResult result = nicknameValidator.validateAvailable("찾아줘토끼1");

            assertThat(result.failure()).isEqualTo(NicknameValidator.Failure.DUPLICATE);
        }
    }
}
