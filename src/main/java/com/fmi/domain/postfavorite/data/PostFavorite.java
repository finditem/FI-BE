package com.fmi.domain.postfavorite.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post_favorite", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favorite_id;

    @CreationTimestamp
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
        isFavorite = true;
    }

    public static PostFavorite create(User user, Post post) {
        return new PostFavorite(user, post);
    }

    public void activate() {
        this.isFavorite = true;
    }

    public void deactivate() {
        this.isFavorite = false;
    }
}
