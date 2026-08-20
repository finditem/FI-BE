package com.fmi.domain.user.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "user_category",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_category",
                    columnNames = {"user_id", "category"})
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_category_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
