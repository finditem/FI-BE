package com.fmi.domain.admin.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 사용자 상세 응답")
public class AdminUserDetailResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "이메일 인증 여부", example = "true")
    private boolean emailVerified;

    @Schema(description = "역할", example = "USER")
    private Role role;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImg;

    @Schema(description = "개인정보 처리방침 동의 여부", example = "true")
    private boolean privacyPolicyAgreed;

    @Schema(description = "이용약관 동의 여부", example = "true")
    private boolean termsOfServiceAgreed;

    @Schema(description = "콘텐츠 활용 동의 여부", example = "true")
    private boolean contentPolicyAgreed;

    @Schema(description = "마케팅 수신 동의 여부", example = "false")
    private boolean marketingConsent;

    @Schema(description = "생성 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2024-01-01T00:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "삭제 시간", example = "null")
    private LocalDateTime deletedAt;

    @Schema(description = "게시글 수", example = "10")
    private long postCount;

    @Schema(description = "댓글 수", example = "25")
    private long commentCount;

    @Schema(description = "신고 수", example = "0")
    private long reportCount;

    @Schema(description = "구독한 카테고리 목록", example = "[\"ELECTRONICS\", \"CLOTHING\"]")
    private List<Category> subscribedCategories;
}
