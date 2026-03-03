package com.fmi.domain.user.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 2, max = 15, message = "닉네임은 2~15자 사이여야 합니다")
    private String nickname;

    @JsonIgnore
    private boolean nicknameProvided = false;

    @JsonSetter("nickname")
    public void setNickname(String nickname) {
        this.nicknameProvided = true;
        this.nickname = nickname;
    }
}
