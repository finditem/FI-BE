package com.fmi.domain.user.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {
    
    @Size(min = 2, max = 15, message = "닉네임은 2~15자 사이여야 합니다")
    private String nickname;  // 닉네임만 수정 가능
}

