package com.fmi.domain.user.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    @Size(min = 2, max = 15, message = "닉네임은 2~15자 사이여야 합니다")
    private String nickname;

    private String profileImageUrl;

    @JsonIgnore
    private boolean profileImageProvided = false;

    @JsonSetter("profileImageUrl")
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageProvided = true;
        this.profileImageUrl = profileImageUrl;
    }
}
