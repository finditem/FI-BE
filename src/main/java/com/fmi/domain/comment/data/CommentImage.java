package com.fmi.domain.comment.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    private CommentImage(String imgUrl, Comment comment) {
        this.imgUrl = imgUrl;
        this.comment = comment;
    }

    public static CommentImage create(String imgUrl, Comment comment) {
        return new CommentImage(imgUrl, comment);
    }
}
