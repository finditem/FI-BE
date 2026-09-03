package com.fmi.domain.user.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String nickname;

    private Boolean deleteProfileImage;

    @JsonIgnore
    private boolean nicknameProvided = false;

    @JsonSetter("nickname")
    public void setNickname(String nickname) {
        this.nicknameProvided = true;
        this.nickname = nickname;
    }

    public boolean isDeleteProfileImage() {
        return Boolean.TRUE.equals(deleteProfileImage);
    }
}
