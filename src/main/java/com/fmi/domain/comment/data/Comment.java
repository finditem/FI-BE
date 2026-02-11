package com.fmi.domain.comment.data;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Comment parent;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    public void delete() {
        this.deleted = true;
        this.content = "삭제된 댓글입니다.";
    }

    private Comment(User user, Post post, String content, Comment parent, int depth) {
        this.user = user;
        this.post = post;
        this.content = content;
        this.parent = parent;
        this.depth = depth;
        this.createdAt = LocalDateTime.now();
        deleted = false;
    }

    public static Comment createComment(User user, Post post, String content) {
        return new Comment(user, post, content, null, 0);
    }

    public static Comment createComment(User user, Post post, String content, Comment parent) {
        if (Objects.isNull(parent)) {
            return createComment(user, post, content);
        }

        int nextDepth = parent.getDepth() + 1;
        if (nextDepth > 2) {
            throw new GeneralException(ErrorStatus._COMMENT_DEPTH_EXCEEDED);
        }

        return new Comment(user, post, content, parent, nextDepth);
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
