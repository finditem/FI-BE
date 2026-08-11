package com.fmi.domain.notice.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notice.data.enums.NoticeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long noticeId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private NoticeCategory category = NoticeCategory.GENERAL;

    @Column(name = "pinned")
    @Builder.Default
    private Boolean pinned = false;

    @Column(name = "view_cnt")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "draft")
    @Builder.Default
    private Boolean draft = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

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

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isNew() {
        return this.createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    public void update(String title, String content, NoticeCategory category, Boolean pinned, Boolean draft) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.pinned = pinned;
        if (draft != null) {
            this.draft = draft;
        }
        this.updatedAt = LocalDateTime.now();
    }
}

