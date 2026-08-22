package com.fmi.domain.user.data;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Email @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified")
    private boolean email_verified;

    @Enumerated(EnumType.STRING) // 여기서 STRING으로 매핑
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "original_password")
    private String originalPassword; // 임시 비밀번호 발급 시 원래 비밀번호 보관

    @Column(name = "temporary_password")
    private String temporaryPassword; // 임시 비밀번호 (해시값)

    @Column(name = "temporary_password_expires_at")
    private LocalDateTime temporaryPasswordExpiresAt; // 임시 비밀번호 만료 시간

    @Column(name = "profile_img")
    private String profile_img;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "withdrawal_reason", length = 200)
    private String withdrawalReason; // 탈퇴 사유 (콤마 구분, 최대 3개)

    @Column(name = "withdrawal_other_reason", columnDefinition = "TEXT")
    private String withdrawalOtherReason; // 탈퇴 사유 기타 (reason에 OTHER가 포함된 경우)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "preferred_language", nullable = false)
    private LanguageCode preferredLanguage = LanguageCode.KO; // 선호 언어 (기본값은 한국어(KO))

    private boolean privacyPolicyAgreed; // 개인정보 처리방침 동의
    private boolean termsOfServiceAgreed; // 이용약관 동의
    private boolean contentPolicyAgreed; // 콘텐츠 활용 동의
    private boolean marketingConsent; // 마케팅 수신 동의

    @OneToMany(mappedBy = "user")
    @Builder.Default
    private List<ChatRoomParticipant> chatRoomParticipants = new ArrayList<>();

    public void updateUserInfo(String email) {
        if (email != null) {
            this.email = email;
        }
    }

    public void issueTemporaryPassword(String encodedPassword, LocalDateTime expiresAt, LocalDateTime now) {
        if (originalPassword == null) {
            originalPassword = password;
        }
        temporaryPassword = encodedPassword;
        temporaryPasswordExpiresAt = expiresAt;
        password = encodedPassword;
        updatedAt = now;
    }

    public boolean restoreExpiredTemporaryPassword(LocalDateTime now) {
        if (temporaryPasswordExpiresAt == null || now.isBefore(temporaryPasswordExpiresAt)) {
            return false;
        }

        if (originalPassword != null) {
            password = originalPassword;
        }
        originalPassword = null;
        temporaryPassword = null;
        temporaryPasswordExpiresAt = null;
        updatedAt = now;
        return true;
    }

    public void changePassword(String encodedPassword, LocalDateTime now) {
        password = encodedPassword;
        originalPassword = null;
        temporaryPassword = null;
        temporaryPasswordExpiresAt = null;
        updatedAt = now;
    }
}
