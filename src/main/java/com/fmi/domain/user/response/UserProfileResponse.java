package com.fmi.domain.user.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId;              // 사용자 ID
    private String nickname;          // 닉네임 (수정 가능)
    private String email;             // 이메일 (조회만)
    private boolean emailVerified;    // 이메일 인증 여부 (조회만)
    private String profileImg;        // 프로필 이미지 URL (수정 가능)
}

