package com.fmi.domain.admin.dto;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.Enum.WithdrawalReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeletedUserResponse {
    private Long userId;
    private String nickname;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private WithdrawalReason withdrawalReason;
    private String withdrawalOtherReason;
}

