package com.fmi.domain.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {

    @NotBlank(message = "새 비밀번호를 입력해주세요") private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해주세요") private String newPasswordConfirm;
}
