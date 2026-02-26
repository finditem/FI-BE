package com.fmi.domain.admin.dto;

import com.fmi.domain.Enum.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 탈퇴 사용자 응답")
public class AdminDeletedUserResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
    @Schema(description = "역할", example = "USER")
    private Role role;
    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "삭제 시간", example = "2024-01-15T00:00:00")
    private LocalDateTime deletedAt;
    @Schema(description = "탈퇴 사유 (콤마 구분, 최대 3개)", example = "NOT_USING,DIFFICULT_TO_USE")
    private String withdrawalReason;
    @Schema(description = "기타 탈퇴 사유 (withdrawalReason에 OTHER가 포함된 경우)", example = "직접 입력한 사유")
    private String withdrawalOtherReason;
}

