package com.fmi.domain.post.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private PostImage(String imgUrl, ImageType imageType, Post post) {
        this.imgUrl = imgUrl;
        this.imageType = imageType;
        this.post = post;
    }

    public static PostImage create(String imgUrl, ImageType imageType, Post post) {
        return new PostImage(imgUrl, imageType, post);
    }
}
