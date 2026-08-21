package com.fmi.domain.user.response;

import com.fmi.domain.Enum.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId; // 사용자 ID
    private String nickname; // 닉네임 (수정 가능)
    private String email; // 이메일 (조회만)
    private String profileImg; // 프로필 이미지 URL (수정 가능)
    private Role role; // 유저 권한 (USER, ADMIN)
    private boolean isSocialUser; // 소셜 로그인 회원 여부 (true: 카카오 등 소셜, false: 일반)
}
