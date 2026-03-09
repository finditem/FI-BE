package com.fmi.domain.noticecomment.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_comment_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NoticeCommentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private NoticeComment comment;

    public static NoticeCommentImage create(String imgUrl, NoticeComment comment) {
        return new NoticeCommentImage(null, imgUrl, comment);
    }
}
