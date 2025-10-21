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
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
        message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String newPassword;
    
    @NotBlank(message = "새 비밀번호 확인을 입력해주세요")
    private String newPasswordConfirm;
}

