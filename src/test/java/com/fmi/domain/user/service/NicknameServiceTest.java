package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fmi.domain.user.service.internal.NicknameValidator;
import com.fmi.domain.user.web.response.CheckResponse;
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
            CheckResponse result = nicknameService.check("찾아줘토끼1");

            assertThat(result.available()).isTrue();
            verify(nicknameValidator).validateAvailable("찾아줘토끼1");
        }
    }
}
