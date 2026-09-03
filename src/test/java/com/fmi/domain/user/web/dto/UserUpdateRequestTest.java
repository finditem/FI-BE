package com.fmi.domain.user.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserUpdateRequest")
class UserUpdateRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("닉네임 정책 위반 여부는 Bean Validation이 아닌 닉네임 검증기가 판단한다")
    void itLeavesNicknamePolicyToNicknameValidator() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("가나다라마바사아자차카");

        assertThat(validator.validate(request)).isEmpty();
    }
}
