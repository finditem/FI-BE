package com.fmi.domain.auth.service.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordGeneratorTest {

    @Test
    @DisplayName("기존 문자 집합으로 12자리 임시 비밀번호를 생성한다")
    void generatesTwelveCharacterTemporaryPassword() {
        // given
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%^&*";

        // when
        String temporaryPassword = passwordGenerator.generateTemporaryPassword();

        // then
        assertThat(temporaryPassword).hasSize(12);
        assertThat(temporaryPassword.chars().allMatch(character -> characters.indexOf(character) >= 0))
                .isTrue();
    }
}
