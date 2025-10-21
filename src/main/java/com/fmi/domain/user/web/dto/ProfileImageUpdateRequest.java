package com.fmi.domain.user.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileImageUpdateRequest {
    private String profileImageUrl; // null이면 삭제, 값이 있으면 업데이트
}

