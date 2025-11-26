package com.fmi.domain.auth.data;


import com.fmi.domain.Enum.Role;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Email
    @Column(name = "email",nullable = true)
    private String email;

    @Column(name = "email_verified")
    private boolean email_verified;

    @Enumerated(EnumType.STRING) // 여기서 STRING으로 매핑
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "original_password")
    private String originalPassword;  // 임시 비밀번호 발급 시 원래 비밀번호 보관

    @Column(name = "temporary_password")
    private String temporaryPassword;  // 임시 비밀번호 (해시값)

    @Column(name = "temporary_password_expires_at")
    private LocalDateTime temporaryPasswordExpiresAt;  // 임시 비밀번호 만료 시간

    @Column(name = "profile_img")
    private String profile_img;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_reason", length = 50)
    private WithdrawalReason withdrawalReason;  // 탈퇴 사유

    @Column(name = "withdrawal_other_reason", columnDefinition = "TEXT")
    private String withdrawalOtherReason;       // 탈퇴 사유 기타 (reason이 OTHER인 경우)

    private boolean termsOfServiceAgreed;      // 서비스 이용약관 동의
    private boolean privacyPolicyAgreed;       // 개인정보 처리방침 동의
    private boolean marketingConsent;           // 마케팅 수신 동의

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<ChatRoomParticipant> chatRoomParticipants = new ArrayList<>();

    public void updateUserInfo(String email) {
        if (email != null) {
            this.email = email;
        }
    }
}

