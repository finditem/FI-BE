package com.fmi.domain.noticelike.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.notice.data.Notice;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "notice_like",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "notice_id"})})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
