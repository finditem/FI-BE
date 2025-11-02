package com.fmi.domain.user.data;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_keyword", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_category_keyword", columnNames = {"user_id", "category", "keyword"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Type category; // Post.postType과 동일 분류 사용

    @Column(name = "keyword", nullable = false, length = 200)
    private String keyword;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}


