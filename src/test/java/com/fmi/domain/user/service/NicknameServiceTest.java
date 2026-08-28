package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.service.internal.NicknameValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NicknameService")
class NicknameServiceTest {

    @Mock
    private NicknameValidator nicknameValidator;

    @InjectMocks
    private NicknameService nicknameService;

    @Nested
    @DisplayName("닉네임 사용 가능 여부를 확인할 때")
    class DescribeCheck {

        @Test
        @DisplayName("닉네임 정책과 중복 검사를 통과하면 사용 가능하다")
        void itReturnsAvailable() {
            when(nicknameValidator.validateAvailable("찾아줘토끼1"))
                    .thenReturn(NicknameValidator.ValidationResult.success());

            NicknameService.CheckResult result = nicknameService.check("찾아줘토끼1");

            assertThat(result.available()).isTrue();
        }

        @Test
        @DisplayName("닉네임 정책을 위반하면 INVALID 결과를 반환한다")
        void itReturnsInvalid() {
            when(nicknameValidator.validateAvailable("_"))
                    .thenReturn(NicknameValidator.ValidationResult.invalid("invalid"));

            NicknameService.CheckResult result = nicknameService.check("_");

            assertThat(result)
                    .extracting(NicknameService.CheckResult::available, NicknameService.CheckResult::errorType)
                    .containsExactly(false, "INVALID");
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 DUPLICATE 결과를 반환한다")
        void itReturnsDuplicate() {
            when(nicknameValidator.validateAvailable("찾아줘토끼1"))
                    .thenReturn(NicknameValidator.ValidationResult.duplicate("duplicate"));

            NicknameService.CheckResult result = nicknameService.check("찾아줘토끼1");

            assertThat(result)
                    .extracting(NicknameService.CheckResult::available, NicknameService.CheckResult::errorType)
                    .containsExactly(false, "DUPLICATE");
        }
    }
}
