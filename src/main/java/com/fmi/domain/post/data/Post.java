package com.fmi.domain.post.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude")
    private double latitude;

    @Column(name = "longitude")
    private double longitude;

    @Column(name = "view_cnt")
    private long viewCount;

    @Column(name = "item_status")
    @Enumerated(EnumType.STRING)
    private PostStatus postStatus;

    @Column(name = "post_type")
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "temporary_save", nullable = false)
    private boolean temporarySave;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "radius")
    private Radius radius;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Post(String title, String address, double latitude, double longitude, PostType postType, Category category, boolean temporarySave, LocalDate date, Radius radius, User user) {
        this.title = title;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.postType = postType;
        this.category = category;
        this.temporarySave = temporarySave;
        this.date = date;
        this.radius = radius;
        this.postStatus = PostStatus.SEARCHING;
        this.createdAt = LocalDateTime.now();
        this.user = user;
        this.viewCount = 0;
    }

    public static Post create(String title, String address, double latitude, double longitude, PostType postType, Category category, boolean temporarySave, LocalDate date, Radius radius, User user) {
        return new Post(title, address, latitude, longitude, postType, category, temporarySave, date, radius, user);
    }

    public boolean isNew() {
        return this.createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }
}
