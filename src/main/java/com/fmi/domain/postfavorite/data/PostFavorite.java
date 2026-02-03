package com.fmi.domain.postfavorite.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favorite_id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_favorite")
    private boolean isFavorite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private PostFavorite(User user, Post post) {
        this.user = user;
        this.post = post;
        createdAt = LocalDateTime.now();
        isFavorite = true;
    }

    public static PostFavorite create(User user, Post post) {
        return new PostFavorite(user, post);
    }

    public void toggleFavorite() {
        isFavorite = !isFavorite;
    }
}
