package com.fmi.domain.notification.data;

import com.fmi.domain.auth.data.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private Long settingsId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 댓글 알림
    @Column(name = "comment_enabled")
    @Builder.Default
    private Boolean commentEnabled = true;

    // 채팅 알림
    @Column(name = "chat_enabled")
    @Builder.Default
    private Boolean chatEnabled = true;

    // 문의 답변 알림
    @Column(name = "inquiry_reply_enabled")
    @Builder.Default
    private Boolean inquiryReplyEnabled = true;

    // 신고 처리 알림
    @Column(name = "report_result_enabled")
    @Builder.Default
    private Boolean reportResultEnabled = true;

    // 좋아요 알림
    @Column(name = "favorite_enabled")
    @Builder.Default
    private Boolean favoriteEnabled = true;

    // 공지사항 알림
    @Column(name = "notice_enabled")
    @Builder.Default
    private Boolean noticeEnabled = true;

    // 카테고리 알림
    @Column(name = "category_enabled")
    @Builder.Default
    private Boolean categoryEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 설정 업데이트
    public void updateSettings(Boolean commentEnabled, Boolean chatEnabled, 
                               Boolean inquiryReplyEnabled, Boolean reportResultEnabled,
                               Boolean favoriteEnabled, Boolean noticeEnabled,
                               Boolean categoryEnabled) {
        if (commentEnabled != null) this.commentEnabled = commentEnabled;
        if (chatEnabled != null) this.chatEnabled = chatEnabled;
        if (inquiryReplyEnabled != null) this.inquiryReplyEnabled = inquiryReplyEnabled;
        if (reportResultEnabled != null) this.reportResultEnabled = reportResultEnabled;
        if (favoriteEnabled != null) this.favoriteEnabled = favoriteEnabled;
        if (noticeEnabled != null) this.noticeEnabled = noticeEnabled;
        if (categoryEnabled != null) this.categoryEnabled = categoryEnabled;
    }
}

