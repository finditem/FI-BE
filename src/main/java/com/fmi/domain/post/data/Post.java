package com.fmi.domain.post.data;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.Status;
import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.postfavorite.data.PostFavorite;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "user_id", nullable = true)
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
    private long viewCnt;

    @Column(name = "item_status")
    @Enumerated(EnumType.STRING)
    private Status itemStatus;

    @Column(name = "post_type")
    @Enumerated(EnumType.STRING)
    private Type postType;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "temporary_save",nullable = false)
    private boolean temporarySave;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "radius")
    private double radius;

    @Builder.Default
    @Column(name="favorite_count")
    private Integer favoriteCount = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<PostImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostFavorite> favorites = new ArrayList<>();

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private Integer commentCount = 0;

    public boolean isNew() {
        return Duration.between(createdAt, LocalDateTime.now()).toHours() < 24;
    }

    public void increaseCommentCount() {
        this.commentCount += 1;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount -= 1;
        }
    }
}
