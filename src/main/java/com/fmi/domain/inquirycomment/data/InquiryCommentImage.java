package com.fmi.domain.inquirycomment.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiry_comment_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InquiryCommentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private InquiryComment comment;

    public static InquiryCommentImage create(String imgUrl, InquiryComment comment) {
        return new InquiryCommentImage(null, imgUrl, comment);
    }
}
