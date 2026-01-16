package com.fmi.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordVerifyRequest {
    
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;
}
