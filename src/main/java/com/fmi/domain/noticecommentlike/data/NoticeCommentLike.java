package com.fmi.domain.noticecommentlike.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.noticecomment.data.NoticeComment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_comment_like", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "comment_id"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private NoticeComment comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
