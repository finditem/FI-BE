package com.fmi.domain.commentlike.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.comment.data.Comment;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"})
)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(name = "is_like")
    private boolean isLike;

    private CommentLike(User user, Comment comment) {
        this.user = user;
        this.comment = comment;
        this.isLike = true;
    }

    public static CommentLike create(User user, Comment comment) {
        return new CommentLike(user, comment);
    }

    public void activate() {
        this.isLike = true;
    }

    public void deactivate() {
        this.isLike = false;
    }
}
