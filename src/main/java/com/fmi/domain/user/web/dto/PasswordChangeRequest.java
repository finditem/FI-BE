package com.fmi.domain.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {
    
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;
    
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?])[A-Za-z\\d!@#$%^&*()\\-_=+\\[{\\]}\\\\|;:'\",<.>/?]{8,16}$",
        message = "비밀번호는 8~16자, 대/소문자·숫자·특수문자를 포함해야 합니다"
    )
    private String newPassword;
    
    @NotBlank(message = "새 비밀번호 확인을 입력해주세요")
    private String newPasswordConfirm;
}

