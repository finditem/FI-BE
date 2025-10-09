package com.fmi.domain.post.data;

import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "view_cnt")
    private String view_cnt;

    @Column(name = "item_status")
    @Enumerated(EnumType.STRING)
    private Status item_status;

    @Column(name = "post_type")
    @Enumerated(EnumType.STRING)
    private Type post_type;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "p_content", nullable = false)
    private String p_content;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
