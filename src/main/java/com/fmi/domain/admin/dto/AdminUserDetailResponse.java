package com.fmi.domain.admin.dto;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponse {

    private Long userId;
    private String name;
    private String nickname;
    private String email;
    private boolean emailVerified;
    private Role role;
    private String profileImg;
    private Long trustScore;
    private boolean termsOfServiceAgreed;
    private boolean privacyPolicyAgreed;
    private boolean marketingConsent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private long postCount;
    private long commentCount;
    private long reportCount;
    private List<Category> subscribedCategories;
}

